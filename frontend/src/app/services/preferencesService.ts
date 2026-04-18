import { cookieService } from "./cookieService";

// --- Cookie Keys ---
const COOKIE_KEYS = {
  VIEW_MODE: "ff_pref_view_mode",
  ITEMS_PER_PAGE: "ff_pref_items_per_page",
  THEME: "ff_pref_theme",
  SIDEBAR_COLLAPSED: "ff_pref_sidebar",
  LAST_VISITED_PAGE: "ff_pref_last_page",
} as const;

// --- Types ---
export type ViewMode = "table" | "statistics" | "cards";
export type Theme = "light" | "dark" | "system";

export interface UserPreferences {
  viewMode: ViewMode;
  itemsPerPage: number;
  theme: Theme;
  sidebarCollapsed: boolean;
  lastVisitedPage: string | null;
}

const DEFAULTS: UserPreferences = {
  viewMode: "table",
  itemsPerPage: 5,
  theme: "system",
  sidebarCollapsed: false,
  lastVisitedPage: null,
};

export const preferencesService = {
  /** Get a single preference, falling back to default. */
  get<K extends keyof UserPreferences>(key: K): UserPreferences[K] {
    const cookieKey = keyToCookie(key);
    const raw = cookieService.get(cookieKey);
    if (raw === null) return DEFAULTS[key];

    return deserialize(key, raw);
  },

  /** Set a single preference. */
  set<K extends keyof UserPreferences>(key: K, value: UserPreferences[K]): void {
    const cookieKey = keyToCookie(key);
    cookieService.set(cookieKey, String(value));
  },

  /** Load all preferences at once. */
  getAll(): UserPreferences {
    return {
      viewMode: this.get("viewMode"),
      itemsPerPage: this.get("itemsPerPage"),
      theme: this.get("theme"),
      sidebarCollapsed: this.get("sidebarCollapsed"),
      lastVisitedPage: this.get("lastVisitedPage"),
    };
  },

  /** Reset all preferences to defaults. */
  resetAll(): void {
    Object.values(COOKIE_KEYS).forEach((key) => cookieService.delete(key));
  },
};

// --- Helpers ---

function keyToCookie(key: keyof UserPreferences): string {
  const map: Record<keyof UserPreferences, string> = {
    viewMode: COOKIE_KEYS.VIEW_MODE,
    itemsPerPage: COOKIE_KEYS.ITEMS_PER_PAGE,
    theme: COOKIE_KEYS.THEME,
    sidebarCollapsed: COOKIE_KEYS.SIDEBAR_COLLAPSED,
    lastVisitedPage: COOKIE_KEYS.LAST_VISITED_PAGE,
  };
  return map[key];
}

function deserialize<K extends keyof UserPreferences>(
  key: K,
  raw: string
): UserPreferences[K] {
  switch (key) {
    case "itemsPerPage": {
      const num = parseInt(raw, 10);
      return (isNaN(num) ? DEFAULTS.itemsPerPage : num) as UserPreferences[K];
    }
    case "sidebarCollapsed":
      return (raw === "true") as UserPreferences[K];
    case "lastVisitedPage":
      return (raw || null) as UserPreferences[K];
    default:
      return raw as UserPreferences[K];
  }
}
