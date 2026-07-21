/** Wire shape of GET /papers, GET /papers/{id} — matches backend/schemas.py::Paper. */
export interface Paper {
  id: string;
  title: string;
  abstract: string;
  citation_count: number;
  year: number;
  pdf_url: string | null;
  doi: string;
  circle_id: string | null;
  is_published: boolean;
  ai_score: number | null;
  journal: string;
}

/**
 * Response of POST /papers/{id}/cite. Cite Circle doesn't model individual
 * citation edges — citing a paper just increments its counter — so this is the
 * only "Citation" shape the API actually returns.
 */
export interface Citation {
  cited: boolean;
  citation_count: number;
}
