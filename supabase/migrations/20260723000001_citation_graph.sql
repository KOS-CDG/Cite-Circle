-- =============================================================================
-- CITATION GRAPH & CO-AUTHOR NETWORK MIGRATION
-- =============================================================================

CREATE TABLE IF NOT EXISTS paper_citations (
    citing_paper_id VARCHAR(100) REFERENCES papers(id) ON DELETE CASCADE,
    cited_paper_id VARCHAR(100) REFERENCES papers(id) ON DELETE CASCADE,
    created_at BIGINT DEFAULT extract(epoch from now()) * 1000,
    PRIMARY KEY (citing_paper_id, cited_paper_id)
);

CREATE INDEX IF NOT EXISTS idx_paper_citations_citing ON paper_citations(citing_paper_id);
CREATE INDEX IF NOT EXISTS idx_paper_citations_cited ON paper_citations(cited_paper_id);
