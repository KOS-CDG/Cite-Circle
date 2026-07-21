/** Mirrors backend/schemas.py::Publication — an OpenAlex-indexed work, distinct from an in-app Paper. */
export interface Publication {
  id: string;
  openalex_id: string;
  doi: string;
  title: string;
  abstract: string;
  journal: string;
  publication_year: number | null;
  citation_count: number;
  work_type: string;
  is_open_access: boolean;
  open_access_url: string;
  source: string;
  first_synced_at: number;
  last_synced_at: number;
}

/** Mirrors backend/schemas.py::Coauthor. */
export interface Coauthor {
  openalex_author_id: string;
  display_name: string;
  orcid_id: string;
  institution: string;
  /** Populated when the co-author is also a Cite Circle member. */
  user_id: string | null;
  shared_publications: number;
}

export type OrcidSyncStatus = 'IDLE' | 'RUNNING' | 'PARTIAL' | 'SUCCESS' | 'FAILED';

/** Response of POST /users/me/orcid/sync. */
export interface OrcidSyncResult {
  status: 'SUCCESS' | 'PARTIAL';
  works_synced: number;
  total_available: number;
  complete: boolean;
  synced_at: number;
  message: string;
}

/** Response of GET /users/me/orcid/state — matches backend/schemas.py::OrcidSyncState. */
export interface OrcidSyncState {
  user_id: string;
  orcid_id: string;
  status: OrcidSyncStatus;
  works_synced: number;
  last_started_at: number | null;
  last_success_at: number | null;
  last_error: string;
  consecutive_failures: number;
  next_eligible_at: number;
}

/**
 * Convenience aggregate for the ORCID section of a profile screen — composed
 * client-side from User + GET /users/me/orcid/state, not a single backend response.
 */
export interface OrcidInfo {
  orcid_id: string;
  orcid_verified: boolean;
  sync_state: OrcidSyncState | null;
  publication_count: number;
}
