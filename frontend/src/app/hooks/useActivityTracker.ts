import { useEffect, useCallback, useRef } from "react";
import { useLocation } from "react-router";
import { activityTracker } from "../services/activityTracker";
import { useCookieConsent } from "./useCookieConsent";

/**
 * Tracks page visits and click events via cookies.
 * Only records data when the user has given cookie consent.
 * Mount once in the Layout component.
 */
export function useActivityTracker() {
  const location = useLocation();
  const { hasConsent } = useCookieConsent();
  const initialised = useRef(false);

  // Start session on first mount (once per app load)
  useEffect(() => {
    if (!hasConsent || initialised.current) return;
    activityTracker.startSession();
    initialised.current = true;
  }, [hasConsent]);

  // Track page visits on route change
  useEffect(() => {
    if (!hasConsent) return;
    activityTracker.trackPageVisit(location.pathname);
  }, [location.pathname, hasConsent]);

  // Keep the session alive on user interaction
  useEffect(() => {
    if (!hasConsent) return;

    const onActivity = () => activityTracker.touch();
    window.addEventListener("click", onActivity);
    window.addEventListener("keydown", onActivity);
    window.addEventListener("scroll", onActivity);

    return () => {
      window.removeEventListener("click", onActivity);
      window.removeEventListener("keydown", onActivity);
      window.removeEventListener("scroll", onActivity);
    };
  }, [hasConsent]);

  const trackClick = useCallback(
    (label: string) => {
      if (!hasConsent) return;
      activityTracker.trackClick(label, location.pathname);
    },
    [hasConsent, location.pathname]
  );

  return { trackClick, getSummary: activityTracker.getSummary.bind(activityTracker) };
}
