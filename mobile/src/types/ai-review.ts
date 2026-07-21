export type AiSuggestionSeverity = 'MINOR' | 'MODERATE' | 'NEEDS_ATTENTION';

/** Matches backend/schemas.py::AiSuggestion. */
export interface AiSuggestion {
  id: string;
  section: string;
  text: string;
  severity: AiSuggestionSeverity;
  is_addressed: boolean;
}

/** Response of POST /papers/review — matches backend/schemas.py::AiReviewReport. */
export interface AiReviewReport {
  id: string;
  paper_id: string;
  paper_title: string;
  model: string;
  overall_score: number;
  structure: number;
  citations: number;
  clarity: number;
  originality: number;
  verdict: string;
  summary: string;
  strengths: string[];
  weaknesses: string[];
  suggestions: AiSuggestion[];
  /** epoch millis */
  created_at: number;
}

/** Alias for the spec's "AiReview" name — same shape as AiReviewReport. */
export type AiReview = AiReviewReport;

/** Body for POST /papers/review — matches backend/schemas.py::ReviewRequest. */
export interface ReviewRequest {
  paper_id?: string | null;
  title?: string | null;
  abstract?: string | null;
}
