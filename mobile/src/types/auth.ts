/** Body for POST /auth/signup — matches backend/schemas.py::SignupRequest. */
export interface SignupRequest {
  email: string;
  password: string;
  name?: string;
  institution?: string;
  field_of_study?: string;
}

/** Body for POST /auth/login — matches backend/schemas.py::LoginRequest. */
export interface LoginRequest {
  email: string;
  password: string;
}

/** Response of POST /auth/signup and /auth/login — matches backend/schemas.py::AuthResponse. */
export interface AuthResponse {
  user_id: string;
  access_token: string;
  refresh_token: string;
}
