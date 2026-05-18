const AUTH_URL = "/api/auth";

export interface AuthUser {
  id: number;
  username: string;
  email: string;
  role: "ADMIN" | "USER";
  permissions: string[];
  /** Signed JWT — attach as `Authorization: Bearer <token>` on every request. */
  token: string;
}

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface RegisterCredentials {
  username: string;
  email: string;
  password: string;
}

export async function loginApi(credentials: LoginCredentials): Promise<AuthUser> {
  const response = await fetch(`${AUTH_URL}/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(credentials),
  });

  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.message || body.detail || "Invalid email or password");
  }

  return response.json();
}

export async function registerApi(credentials: RegisterCredentials): Promise<AuthUser> {
  const response = await fetch(`${AUTH_URL}/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(credentials),
  });

  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.message || body.detail || "Registration failed");
  }

  return response.json();
}
