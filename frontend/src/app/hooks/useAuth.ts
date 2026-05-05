import { useState, useCallback } from "react";
import { loginApi, registerApi, type AuthUser, type LoginCredentials, type RegisterCredentials } from "../services/authService";

const SESSION_KEY = "formforge_user";

function getStoredUser(): AuthUser | null {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY);
    return raw ? (JSON.parse(raw) as AuthUser) : null;
  } catch {
    return null;
  }
}

function storeUser(user: AuthUser): void {
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(user));
}

function clearUser(): void {
  sessionStorage.removeItem(SESSION_KEY);
}

export function useAuth() {
  const [user, setUser] = useState<AuthUser | null>(getStoredUser);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const login = useCallback(async (credentials: LoginCredentials) => {
    setLoading(true);
    setError(null);
    try {
      const authUser = await loginApi(credentials);
      storeUser(authUser);
      setUser(authUser);
      return authUser;
    } catch (err) {
      const message = err instanceof Error ? err.message : "Login failed";
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const register = useCallback(async (credentials: RegisterCredentials) => {
    setLoading(true);
    setError(null);
    try {
      const authUser = await registerApi(credentials);
      storeUser(authUser);
      setUser(authUser);
      return authUser;
    } catch (err) {
      const message = err instanceof Error ? err.message : "Registration failed";
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    clearUser();
    setUser(null);
    setError(null);
  }, []);

  const hasPermission = useCallback(
    (permission: string) => user?.permissions.includes(permission) ?? false,
    [user]
  );

  return { user, error, loading, login, register, logout, hasPermission };
}

/** Read-only helper – does not re-render on change. Use for route guards. */
export function getCurrentUser(): AuthUser | null {
  return getStoredUser();
}
