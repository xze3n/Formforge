import { useState, useCallback } from "react";
import {
  preferencesService,
  type UserPreferences,
  type ViewMode,
  type Theme,
} from "../services/preferencesService";
import { useCookieConsent } from "./useCookieConsent";

/**
 * React hook for reading and writing user preferences stored in cookies.
 * Falls back to in-memory defaults when consent is not granted.
 */
export function usePreferences() {
  const { hasConsent } = useCookieConsent();
  const [prefs, setPrefs] = useState<UserPreferences>(() =>
    hasConsent ? preferencesService.getAll() : preferencesService.getAll()
  );

  const updatePref = useCallback(
    <K extends keyof UserPreferences>(key: K, value: UserPreferences[K]) => {
      if (hasConsent) {
        preferencesService.set(key, value);
      }
      setPrefs((prev) => ({ ...prev, [key]: value }));
    },
    [hasConsent]
  );

  const setViewMode = useCallback(
    (mode: ViewMode) => updatePref("viewMode", mode),
    [updatePref]
  );

  const setItemsPerPage = useCallback(
    (count: number) => updatePref("itemsPerPage", count),
    [updatePref]
  );

  const setTheme = useCallback(
    (theme: Theme) => updatePref("theme", theme),
    [updatePref]
  );

  const setLastVisitedPage = useCallback(
    (path: string) => updatePref("lastVisitedPage", path),
    [updatePref]
  );

  const resetPreferences = useCallback(() => {
    preferencesService.resetAll();
    setPrefs(preferencesService.getAll());
  }, []);

  return {
    preferences: prefs,
    setViewMode,
    setItemsPerPage,
    setTheme,
    setLastVisitedPage,
    resetPreferences,
  };
}
