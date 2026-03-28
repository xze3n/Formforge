import { useState, useCallback } from "react";
import { cookieService } from "../services/cookieService";
import { activityTracker } from "../services/activityTracker";

const CONSENT_COOKIE = "ff_cookie_consent";

export type ConsentStatus = "granted" | "denied" | "pending";

/**
 * Manages cookie-consent state.
 * - "pending"  → banner should be shown
 * - "granted"  → tracking & preference cookies are allowed
 * - "denied"   → only essential cookies; all app cookies cleared
 */
export function useCookieConsent() {
  const [status, setStatus] = useState<ConsentStatus>(() => {
    const stored = cookieService.get(CONSENT_COOKIE);
    if (stored === "granted" || stored === "denied") return stored;
    return "pending";
  });

  const grant = useCallback(() => {
    cookieService.set(CONSENT_COOKIE, "granted", {
      maxAge: 60 * 60 * 24 * 365, // 1 year
    });
    setStatus("granted");
  }, []);

  const deny = useCallback(() => {
    // Clear all existing app cookies first
    cookieService.clearAllAppCookies();
    // Then set the consent cookie to "denied" (this one is essential)
    cookieService.set(CONSENT_COOKIE, "denied", {
      maxAge: 60 * 60 * 24 * 365,
    });
    setStatus("denied");
  }, []);

  const revoke = useCallback(() => {
    activityTracker.clearAll();
    cookieService.clearAllAppCookies();
    cookieService.delete(CONSENT_COOKIE);
    setStatus("pending");
  }, []);

  return {
    status,
    hasConsent: status === "granted",
    isPending: status === "pending",
    grant,
    deny,
    revoke,
  };
}
