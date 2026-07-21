export type PostType = 'DISCUSSION' | 'PAPER_SHARE' | 'MILESTONE' | 'CIRCLE_ACTIVITY';
export type PostFlair = 'QUESTION' | 'DISCUSSION' | 'PAPER_FEEDBACK' | 'RESOURCE' | 'NONE';

/** Wire shape of GET /posts, GET /posts/saved, GET /posts/circle/{id} — matches backend/schemas.py::Post. */
export interface Post {
  id: string;
  author_id: string;
  content: string;
  type: PostType;
  /** epoch millis */
  timestamp: number;
  endorse_count: number;
  comment_count: number;
  circle_id: string | null;
  attached_paper_id: string | null;
  milestone_text: string | null;
  flair: PostFlair;
  image_url: string | null;
  is_endorsed: boolean;
  is_saved: boolean;
}

/** Body for POST /posts — matches backend/schemas.py::PostCreate. */
export interface PostCreate {
  content: string;
  type?: PostType;
  circle_id?: string | null;
  attached_paper_id?: string | null;
  milestone_text?: string | null;
  flair?: PostFlair;
  image_url?: string | null;
}

/** Response of POST /posts/{id}/save. */
export interface SavePostResult {
  saved: boolean;
}

/** Response of POST /posts/{id}/endorse. */
export interface EndorsePostResult {
  endorsed: boolean;
  endorse_count: number;
}
