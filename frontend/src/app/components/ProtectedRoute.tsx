import { Navigate, Outlet } from "react-router";
import { getCurrentUser } from "../hooks/useAuth";

/**
 * Wraps routes that require the user to be logged in.
 * If there is no session, redirects to /login with a `from` state so the
 * login page can redirect back after a successful login.
 */
export function ProtectedRoute() {
  const user = getCurrentUser();

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}
