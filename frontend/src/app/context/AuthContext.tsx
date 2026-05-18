import { createContext, useContext, useState, useCallback, useEffect, useRef, type ReactNode } from "react";
import { loginApi, registerApi, type AuthUser, type LoginCredentials, type RegisterCredentials } from "../services/authService";

const SESSION_KEY = "formforge_user";

/**
 * Inactivity timeout in milliseconds.
 * Matches the JWT expiration (30 minutes). The user is automatically logged out
 * when no mouse, keyboard, or touch activity is detected for this duration.
 */
const INACTIVITY_TIMEOUT_MS = 30 * 60 * 1000;

interface AuthContextValue {
  user: AuthUser | null;
  error: string | null;
  loading: boolean;
  login: (credentials: LoginCredentials) => Promise<AuthUser>;
  register: (credentials: RegisterCredentials) => Promise<AuthUser>;
  logout: () => void;
  hasPermission: (permission: string) => boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => {
    try {
      const raw = sessionStorage.getItem(SESSION_KEY);
      return raw ? (JSON.parse(raw) as AuthUser) : null;
    } catch {
      return null;
    }
  });
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  // ── inactivity logout ──────────────────────────────────────────────────────
  const inactivityTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const clearInactivityTimer = useCallback(() => {
    if (inactivityTimer.current !== null) {
      clearTimeout(inactivityTimer.current);
      inactivityTimer.current = null;
    }
  }, []);

  const resetInactivityTimer = useCallback((currentUser: AuthUser | null) => {
    clearInactivityTimer();
    if (!currentUser) return;
    inactivityTimer.current = setTimeout(() => {
      // Logout happens inside the effect; reference the setter directly to
      // avoid a stale-closure dependency on `logout`.
      sessionStorage.removeItem(SESSION_KEY);
      setUser(null);
      setError(null);
    }, INACTIVITY_TIMEOUT_MS);
  }, [clearInactivityTimer]);

  // Register activity listeners whenever there is an authenticated user.
  useEffect(() => {
    if (!user) {
      clearInactivityTimer();
      return;
    }

    // Start the timer for the current session.
    resetInactivityTimer(user);

    const onActivity = () => resetInactivityTimer(user);
    const events = ["mousemove", "mousedown", "keydown", "touchstart", "scroll"] as const;
    events.forEach((ev) => window.addEventListener(ev, onActivity, { passive: true }));

    return () => {
      clearInactivityTimer();
      events.forEach((ev) => window.removeEventListener(ev, onActivity));
    };
  }, [user, resetInactivityTimer, clearInactivityTimer]);

  // ── auth actions ───────────────────────────────────────────────────────────

  const login = useCallback(async (credentials: LoginCredentials) => {
    setLoading(true);
    setError(null);
    try {
      const authUser = await loginApi(credentials);
      sessionStorage.setItem(SESSION_KEY, JSON.stringify(authUser));
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
      sessionStorage.setItem(SESSION_KEY, JSON.stringify(authUser));
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
    clearInactivityTimer();
    sessionStorage.removeItem(SESSION_KEY);
    setUser(null);
    setError(null);
  }, [clearInactivityTimer]);

  const hasPermission = useCallback(
    (permission: string) => user?.permissions.includes(permission) ?? false,
    [user]
  );

  return (
    <AuthContext.Provider value={{ user, error, loading, login, register, logout, hasPermission }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuthContext(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuthContext must be used inside AuthProvider");
  return ctx;
}
