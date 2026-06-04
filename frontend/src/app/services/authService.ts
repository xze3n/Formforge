const API_BASE = import.meta.env.VITE_API_URL ?? "";
const AUTH_URL = `${API_BASE}/api/auth`;

export interface AuthUser {
  id: number;
  username: string;
  email: string;
  role: "ADMIN" | "USER";
  permissions: string[];
  /** Signed JWT — attach as `Authorization: Bearer <token>` on every request. */
  token: string;
  /** Opaque refresh token for obtaining new access tokens. */
  refreshToken: string;
}

export interface LoginCredentials {
  /** Email address or username */
  identifier: string;
  password: string;
}

export interface RegisterCredentials {
  username: string;
  email: string;
  password: string;
}

export interface TwoFaRequiredResponse {
  twoFactorRequired: true;
  message: string;
  devCode?: string;
}

export async function loginApi(credentials: LoginCredentials): Promise<TwoFaRequiredResponse> {
  const response = await fetch(`${AUTH_URL}/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(credentials),
  });

  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.message || body.detail || "Invalid credentials");
  }

  return response.json();
}

export async function verifyTwoFaApi(token: string): Promise<AuthUser> {
  const response = await fetch(`${AUTH_URL}/verify-2fa`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ token }),
  });

  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.message || body.detail || "Invalid or expired verification code");
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

export async function refreshTokenApi(refreshToken: string): Promise<AuthUser> {
  const response = await fetch(`${AUTH_URL}/refresh`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
  });

  if (!response.ok) {
    throw new Error("Session expired. Please log in again.");
  }

  return response.json();
}

export async function logoutApi(refreshToken: string): Promise<void> {
  await fetch(`${AUTH_URL}/logout`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
  }).catch(() => {/* best-effort */});
}

export async function forgotPasswordApi(email: string): Promise<{ message: string; resetToken?: string }> {
  const response = await fetch(`${AUTH_URL}/forgot-password`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email }),
  });

  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.message || "Request failed");
  }

  return response.json();
}

export async function resetPasswordApi(token: string, newPassword: string): Promise<void> {
  const response = await fetch(`${AUTH_URL}/reset-password`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ token, newPassword }),
  });

  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.message || body.detail || "Password reset failed");
  }
}
