import type { Circle } from './circle';
import type { Paper } from './paper';
import type { Post } from './post';
import type { User } from './user';

/** Response of GET /search — matches backend/schemas.py::SearchResults. */
export interface SearchResults {
  people: User[];
  papers: Paper[];
  circles: Circle[];
  posts: Post[];
}
