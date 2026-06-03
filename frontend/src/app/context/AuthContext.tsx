import { createContext, useContext, useState, useCallback, useEffect, useRef, type ReactNode } from "react";
import {
  loginApi, registerApi, logoutApi, forgotPasswordApi, resetPasswordApi, verifyTwoFaApi,
  type AuthUser, type LoginCredentials, type RegisterCredentials, type TwoFaRequiredResponse,
} from "../services/authService";

const SESSION_KEY = "formforge_user";

/**
 * Inactivity timeout in milliseconds.
 * The user is automatically logged out when no mouse, keyboard, or touch
 * activity is detected for this duration.
 */
const INACTIVITY_TIMEOUT_MS = 10 * 60 * 1000; // 10 minutes

interface AuthContextValue {
  user: AuthUser | null;
  error: string | null;
  loading: boolean;
  login: (credentials: LoginCredentials) => Promise<TwoFaRequiredResponse>;
  verifyTwoFa: (token: string) => Promise<AuthUser>;
  register: (credentials: RegisterCredentials) => Promise<AuthUser>;
  logout: () => void;
  forgotPassword: (email: string) => Promise<{ message: string; resetToken?: string }>;
  resetPassword: (token: string, newPassword: string) => Promise<void>;
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
      if (currentUser.refreshToken) logoutApi(currentUser.refreshToken);
      sessionStorage.removeItem(SESSION_KEY);
      window.location.reload();
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

  // ── session-expired event (HTTP 401 after failed refresh) ─────────────────
  useEffect(() => {
    const onExpired = () => {
      clearInactivityTimer();
      sessionStorage.removeItem(SESSION_KEY);
      setUser(null);
      setError(null);
      window.location.href = "/login";
    };
    window.addEventListener("ff:auth:expired", onExpired);
    return () => window.removeEventListener("ff:auth:expired", onExpired);
  }, [clearInactivityTimer]);

  // ── auth actions ───────────────────────────────────────────────────────────

  const login = useCallback(async (credentials: LoginCredentials) => {
    setLoading(true);
    setError(null);
    try {
      return await loginApi(credentials);
    } catch (err) {
      const message = err instanceof Error ? err.message : "Login failed";
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const verifyTwoFa = useCallback(async (token: string) => {
    setLoading(true);
    setError(null);
    try {
      const authUser = await verifyTwoFaApi(token);
      sessionStorage.setItem(SESSION_KEY, JSON.stringify(authUser));
      setUser(authUser);
      return authUser;
    } catch (err) {
      const message = err instanceof Error ? err.message : "Verification failed";
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
    const raw = sessionStorage.getItem(SESSION_KEY);
    if (raw) {
      try {
        const stored = JSON.parse(raw) as AuthUser;
        if (stored.refreshToken) logoutApi(stored.refreshToken);
      } catch { /* ignore */ }
    }
    sessionStorage.removeItem(SESSION_KEY);
    setUser(null);
    setError(null);
  }, [clearInactivityTimer]);

  const forgotPassword = useCallback(async (email: string) => {
    return forgotPasswordApi(email);
  }, []);

  const resetPassword = useCallback(async (token: string, newPassword: string) => {
    return resetPasswordApi(token, newPassword);
  }, []);

  const hasPermission = useCallback(
    (permission: string) => user?.permissions.includes(permission) ?? false,
    [user]
  );

  return (
    <AuthContext.Provider value={{ user, error, loading, login, verifyTwoFa, register, logout, forgotPassword, resetPassword, hasPermission }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuthContext(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuthContext must be used inside AuthProvider");
  return ctx;
}
