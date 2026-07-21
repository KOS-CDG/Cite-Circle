export type UserRole = 'STUDENT' | 'EDUCATOR' | 'RESEARCHER' | 'ADMIN';

/** Wire shape of GET /users/me, GET /users/{id} — matches backend/schemas.py::User. */
export interface User {
  id: string;
  name: string;
  avatar_url: string;
  role: UserRole;
  institution: string;
  field_of_study: string;
  bio: string;
  orcid_id: string;
  follower_count: number;
  following_count: number;
  citation_count: number;
  /** Citations on externally-indexed work, kept separate from citation_count (in-app /papers/{id}/cite). */
  external_citation_count: number;
  publication_count: number;
  orcid_verified: boolean;
  is_verified: boolean;
  interests: string[];
}

/** Body for PUT /users/me — matches backend/schemas.py::UserUpdate. */
export interface UserUpdate {
  name?: string;
  avatar_url?: string;
  institution?: string;
  field_of_study?: string;
  bio?: string;
  orcid_id?: string;
  interests?: string[];
}

/** Academic-standing subset of User, for profile header / credentials cards. */
export type AcademicCredentials = Pick<
  User,
  | 'institution'
  | 'field_of_study'
  | 'orcid_id'
  | 'orcid_verified'
  | 'publication_count'
  | 'citation_count'
  | 'external_citation_count'
>;

/**
 * Profile-screen view of a user. The backend only ever returns the plain User
 * shape (no relationship flags) — is_following/is_connected/connection_pending
 * are not part of any response body and must be hydrated client-side from the
 * follow/connect endpoints' results, the same way the Kotlin app's repository
 * layer does it.
 */
export interface Profile extends User {
  is_following?: boolean;
  is_connected?: boolean;
  connection_pending?: boolean;
}

/** Response of POST /users/{id}/follow. */
export interface FollowResult {
  following: boolean;
}

/** Response of POST /users/{id}/connect, /accept, /decline. */
export interface ConnectionResult {
  status: 'PENDING' | 'ACCEPTED' | 'DECLINED';
}
