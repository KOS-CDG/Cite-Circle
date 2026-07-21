/**
 * backend/schemas.py types this field as a plain str, but every value written
 * server-side (see main.py's notification inserts) comes from this fixed set,
 * matching the Kotlin app's NotifType enum.
 */
export type NotificationType =
  | 'ENDORSEMENT'
  | 'COMMENT'
  | 'CONNECTION'
  | 'CIRCLE_INVITE'
  | 'AI_APPROVED'
  | 'CITATION'
  | 'NEW_FOLLOWER';

/** Wire shape of GET /notifications — matches backend/schemas.py::Notification. */
export interface Notification {
  id: string;
  type: NotificationType;
  actor_id: string;
  receiver_id: string;
  content: string;
  /** epoch millis */
  timestamp: number;
  is_read: boolean;
  target_id: string;
}
