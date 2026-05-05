import { useAuthContext } from "../context/AuthContext";
import type { AuthUser } from "../services/authService";

/** Shared auth state via context — all components see the same user. */
export { useAuthContext as useAuth };

/** Read-only helper – does not re-render on change. Use for route guards only. */
export function getCurrentUser(): AuthUser | null {
  try {
    const raw = sessionStorage.getItem("formforge_user");
    return raw ? (JSON.parse(raw) as AuthUser) : null;
  } catch {
    return null;
  }
}
