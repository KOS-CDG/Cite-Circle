/** Wire shape of GET /posts/{id}/comments — matches backend/schemas.py::Comment. */
export interface Comment {
  id: string;
  author_id: string;
  post_id: string;
  content: string;
  /** epoch millis */
  timestamp: number;
  like_count: number;
  parent_id: string | null;
}

/** Body for POST /posts/{id}/comments — matches backend/schemas.py::CommentCreate. */
export interface CommentCreate {
  content: string;
  parent_id?: string | null;
}
