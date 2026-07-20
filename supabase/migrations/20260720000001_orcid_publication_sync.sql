-- =============================================================================
-- Phase 1.2 — ORCID / OpenAlex publication + citation sync
--
-- ADDITIVE ONLY. This migration never drops or rewrites anything created by
-- 20260719000001_init_cite_circle.sql, which is destructive (DROP ... CASCADE on
-- every table) and must not be re-run against an environment holding real data.
--
-- Design notes:
--   * `publications` is deliberately separate from `papers`. `papers` holds
--     in-app content (circle_id, ai_score, pdf upload, publish flow); this table
--     holds a researcher's external bibliography mirrored from OpenAlex. Merging
--     them would put every synced work into the library and feed.
--   * Rows are keyed by the OpenAlex short work id (e.g. 'W2741809807') so a work
--     co-authored by several Cite-Circle users is stored exactly once.
--   * `users.citation_count` is incrementally maintained by the in-app
--     /papers/{id}/cite endpoint. It is NOT touched here. External citations land
--     in the new `users.external_citation_count`, kept in sync by trigger.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. USER COLUMNS
-- -----------------------------------------------------------------------------

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS external_citation_count INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS publication_count INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS orcid_verified BOOLEAN DEFAULT FALSE;

COMMENT ON COLUMN users.external_citation_count IS
    'Sum of cited_by_count across linked OpenAlex publications. Maintained by trigger; distinct from the in-app citation_count.';
COMMENT ON COLUMN users.orcid_verified IS
    'TRUE once an ORCID sync has successfully matched works to this orcid_id. Not an identity proof — see Phase 1.3 for institutional verification.';

-- -----------------------------------------------------------------------------
-- 2. PUBLICATIONS (mirrored external works)
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS publications (
    id                 VARCHAR(100) PRIMARY KEY,          -- OpenAlex short id, e.g. 'W2741809807'
    openalex_id        VARCHAR(255) NOT NULL,             -- full canonical URL
    doi                VARCHAR(255) DEFAULT '',
    title              VARCHAR(1024) NOT NULL,
    abstract           TEXT DEFAULT '',
    journal            VARCHAR(512) DEFAULT '',
    publication_year   INT,
    citation_count     INT DEFAULT 0,
    work_type          VARCHAR(64) DEFAULT '',            -- article, book-chapter, preprint, ...
    is_open_access     BOOLEAN DEFAULT FALSE,
    open_access_url    VARCHAR(1024) DEFAULT '',
    source             VARCHAR(32) DEFAULT 'OPENALEX',    -- room for CROSSREF / S2 later
    first_synced_at    BIGINT NOT NULL,                   -- epoch ms, matches Android Long
    last_synced_at     BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_publications_doi ON publications(doi) WHERE doi <> '';
CREATE INDEX IF NOT EXISTS idx_publications_year ON publications(publication_year DESC);
CREATE INDEX IF NOT EXISTS idx_publications_citations ON publications(citation_count DESC);

-- -----------------------------------------------------------------------------
-- 3. PUBLICATION AUTHORS (co-authorship edges)
--
-- One row per author position on a work. `user_id` is NULL for co-authors who
-- are not Cite-Circle members — keeping them is what makes the Phase 2
-- co-authorship graph possible without a second backfill.
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS publication_authors (
    publication_id      VARCHAR(100) NOT NULL REFERENCES publications(id) ON DELETE CASCADE,
    author_position     INT NOT NULL DEFAULT 0,
    openalex_author_id  VARCHAR(255) NOT NULL,
    display_name        VARCHAR(512) NOT NULL,
    orcid_id            VARCHAR(100) DEFAULT '',
    institution         VARCHAR(512) DEFAULT '',
    is_corresponding    BOOLEAN DEFAULT FALSE,
    user_id             VARCHAR(100) REFERENCES users(id) ON DELETE SET NULL,
    PRIMARY KEY (publication_id, openalex_author_id)
);

CREATE INDEX IF NOT EXISTS idx_pub_authors_user ON publication_authors(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_pub_authors_orcid ON publication_authors(orcid_id) WHERE orcid_id <> '';
CREATE INDEX IF NOT EXISTS idx_pub_authors_publication ON publication_authors(publication_id);

-- -----------------------------------------------------------------------------
-- 4. SYNC STATE
--
-- Cron-ready: the on-demand endpoint writes the same row a future batch worker
-- reads. `next_eligible_at` + `consecutive_failures` give a scheduler its
-- backoff and its work queue with no further migration.
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS orcid_sync_state (
    user_id              VARCHAR(100) PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    orcid_id             VARCHAR(100) NOT NULL,
    -- IDLE | RUNNING (in flight) | PARTIAL (paged out, resumable) | SUCCESS | FAILED
    status               VARCHAR(32) NOT NULL DEFAULT 'IDLE',
    cursor               VARCHAR(512) DEFAULT '',              -- OpenAlex next_cursor for resumable paging
    works_synced         INT DEFAULT 0,
    last_started_at      BIGINT,
    last_success_at      BIGINT,
    last_error           TEXT DEFAULT '',
    consecutive_failures INT DEFAULT 0,
    next_eligible_at     BIGINT DEFAULT 0                      -- epoch ms; scheduler picks up rows at/after this
);

CREATE INDEX IF NOT EXISTS idx_orcid_sync_due ON orcid_sync_state(next_eligible_at)
    WHERE status <> 'RUNNING';

-- -----------------------------------------------------------------------------
-- 5. DERIVED COUNT MAINTENANCE
--
-- Recomputes rather than increments: sync is idempotent and re-runs overwrite
-- citation_count in place, so a delta-based trigger would drift.
-- -----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION recompute_user_publication_stats(target_user_id VARCHAR(100))
RETURNS VOID AS $$
BEGIN
    UPDATE users u
    SET external_citation_count = COALESCE(agg.total_citations, 0),
        publication_count       = COALESCE(agg.total_works, 0)
    FROM (
        SELECT
            SUM(p.citation_count) AS total_citations,
            COUNT(*)              AS total_works
        FROM publication_authors pa
        JOIN publications p ON p.id = pa.publication_id
        WHERE pa.user_id = target_user_id
    ) agg
    WHERE u.id = target_user_id;
END;
$$ LANGUAGE plpgsql;

-- Author link added/removed → refresh that user.
CREATE OR REPLACE FUNCTION trg_publication_authors_stats()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'DELETE') THEN
        IF OLD.user_id IS NOT NULL THEN
            PERFORM recompute_user_publication_stats(OLD.user_id);
        END IF;
        RETURN NULL;
    END IF;

    IF NEW.user_id IS NOT NULL THEN
        PERFORM recompute_user_publication_stats(NEW.user_id);
    END IF;
    -- Re-link (author claimed by a different account) must also refresh the old owner.
    IF (TG_OP = 'UPDATE') AND OLD.user_id IS NOT NULL AND OLD.user_id IS DISTINCT FROM NEW.user_id THEN
        PERFORM recompute_user_publication_stats(OLD.user_id);
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_pub_authors_stats ON publication_authors;
CREATE TRIGGER trg_pub_authors_stats
AFTER INSERT OR UPDATE OR DELETE ON publication_authors
FOR EACH ROW EXECUTE FUNCTION trg_publication_authors_stats();

-- Citation count changed on a work → refresh every linked user.
CREATE OR REPLACE FUNCTION trg_publications_stats()
RETURNS TRIGGER AS $$
DECLARE
    linked_user VARCHAR(100);
BEGIN
    IF NEW.citation_count IS DISTINCT FROM OLD.citation_count THEN
        FOR linked_user IN
            SELECT user_id FROM publication_authors
            WHERE publication_id = NEW.id AND user_id IS NOT NULL
        LOOP
            PERFORM recompute_user_publication_stats(linked_user);
        END LOOP;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_publications_citation_stats ON publications;
CREATE TRIGGER trg_publications_citation_stats
AFTER UPDATE ON publications
FOR EACH ROW EXECUTE FUNCTION trg_publications_stats();

-- -----------------------------------------------------------------------------
-- 6. ROW LEVEL SECURITY
--
-- Defence in depth only. The Android client never connects to Postgres directly
-- — it calls the FastAPI service, which holds the service-role key and bypasses
-- RLS. Real authorization lives in the request handlers. These policies exist so
-- that an anon-key leak cannot mutate the bibliography.
-- -----------------------------------------------------------------------------

ALTER TABLE publications        ENABLE ROW LEVEL SECURITY;
ALTER TABLE publication_authors ENABLE ROW LEVEL SECURITY;
ALTER TABLE orcid_sync_state    ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS publications_public_read ON publications;
CREATE POLICY publications_public_read ON publications
    FOR SELECT USING (TRUE);

DROP POLICY IF EXISTS publication_authors_public_read ON publication_authors;
CREATE POLICY publication_authors_public_read ON publication_authors
    FOR SELECT USING (TRUE);

-- Sync state is operational metadata: readable only by its owner.
DROP POLICY IF EXISTS orcid_sync_state_owner_read ON orcid_sync_state;
CREATE POLICY orcid_sync_state_owner_read ON orcid_sync_state
    FOR SELECT USING (auth.uid()::text = user_id);

-- No INSERT/UPDATE/DELETE policies: writes are service-role only, by design.
