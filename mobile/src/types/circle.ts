/** Wire shape of GET /circles, GET /circles/{id} — matches backend/schemas.py::Circle. */
export interface Circle {
  id: string;
  name: string;
  description: string;
  icon_emoji: string;
  /** packed ARGB color */
  banner_color: number;
  member_count: number;
  category: string;
  post_count: number;
  is_joined: boolean | null;
}

/** Response of POST /circles/{id}/join and /leave. */
export interface CircleMembershipResult {
  is_joined: boolean;
  member_count: number;
}
