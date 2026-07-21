/** Wire shape of GET /conversations — matches backend/schemas.py::Conversation. */
export interface Conversation {
  id: string;
  created_at: string | null;
}

/** Wire shape of GET/POST /conversations/{id}/messages — matches backend/schemas.py::Message. */
export interface Message {
  id: string;
  conversation_id: string;
  sender_id: string;
  content: string;
  /** epoch millis */
  timestamp: number;
  is_read: boolean;
  attached_paper_id: string | null;
}

/** Body for POST /conversations/{id}/messages — matches backend/schemas.py::MessageCreate. */
export interface MessageCreate {
  content: string;
  attached_paper_id?: string | null;
}
