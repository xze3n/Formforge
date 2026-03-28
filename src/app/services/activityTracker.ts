import { cookieService } from "./cookieService";

// --- Cookie Keys ---
const COOKIE_KEYS = {
  PAGE_VISITS: "ff_page_visits",
  SESSION_START: "ff_session_start",
  LAST_ACTIVE: "ff_last_active",
  TOTAL_SESSIONS: "ff_total_sessions",
  CLICK_EVENTS: "ff_click_events",
  MOST_VISITED: "ff_most_visited",
} as const;

// --- Types ---
export interface PageVisit {
  path: string;
  timestamp: number;
}

export interface ClickEvent {
  label: string;
  path: string;
  timestamp: number;
}

export interface ActivitySummary {
  pageVisits: PageVisit[];
  clickEvents: ClickEvent[];
  totalSessions: number;
  currentSessionStart: number | null;
  lastActiveTimestamp: number | null;
  mostVisitedPages: { path: string; count: number }[];
}

// Max entries stored per cookie to keep size in check (4 KB limit per cookie)
const MAX_PAGE_VISITS = 50;
const MAX_CLICK_EVENTS = 30;

export const activityTracker = {
  /** Start or resume a session. Call once on app mount. */
  startSession(): void {
    const existing = cookieService.get(COOKIE_KEYS.SESSION_START);
    if (!existing) {
      cookieService.set(COOKIE_KEYS.SESSION_START, String(Date.now()), {
        maxAge: 60 * 30, // session cookie: 30 min inactivity = new session
      });

      // Increment total sessions (persistent cookie)
      const total = Number(cookieService.get(COOKIE_KEYS.TOTAL_SESSIONS) || "0");
      cookieService.set(COOKIE_KEYS.TOTAL_SESSIONS, String(total + 1));
    }

    this.touch();
  },

  /** Update last-active timestamp. */
  touch(): void {
    cookieService.set(COOKIE_KEYS.LAST_ACTIVE, String(Date.now()), {
      maxAge: 60 * 30,
    });
  },

  /** Record a page visit. */
  trackPageVisit(path: string): void {
    const visits = cookieService.getJSON<PageVisit[]>(COOKIE_KEYS.PAGE_VISITS) || [];
    visits.push({ path, timestamp: Date.now() });

    // Trim oldest entries if over limit
    const trimmed = visits.slice(-MAX_PAGE_VISITS);
    cookieService.setJSON(COOKIE_KEYS.PAGE_VISITS, trimmed);

    // Update most-visited aggregation
    this.updateMostVisited(path);
  },

  /** Record a user click event (buttons, links, actions). */
  trackClick(label: string, path: string): void {
    const events = cookieService.getJSON<ClickEvent[]>(COOKIE_KEYS.CLICK_EVENTS) || [];
    events.push({ label, path, timestamp: Date.now() });

    const trimmed = events.slice(-MAX_CLICK_EVENTS);
    cookieService.setJSON(COOKIE_KEYS.CLICK_EVENTS, trimmed);
  },

  /** Get the full activity summary. */
  getSummary(): ActivitySummary {
    return {
      pageVisits: cookieService.getJSON<PageVisit[]>(COOKIE_KEYS.PAGE_VISITS) || [],
      clickEvents: cookieService.getJSON<ClickEvent[]>(COOKIE_KEYS.CLICK_EVENTS) || [],
      totalSessions: Number(cookieService.get(COOKIE_KEYS.TOTAL_SESSIONS) || "0"),
      currentSessionStart: this.getSessionStart(),
      lastActiveTimestamp: this.getLastActive(),
      mostVisitedPages: cookieService.getJSON<{ path: string; count: number }[]>(
        COOKIE_KEYS.MOST_VISITED
      ) || [],
    };
  },

  /** Clear all activity data. */
  clearAll(): void {
    Object.values(COOKIE_KEYS).forEach((key) => cookieService.delete(key));
  },

  // --- Private helpers ---

  getSessionStart(): number | null {
    const raw = cookieService.get(COOKIE_KEYS.SESSION_START);
    return raw ? Number(raw) : null;
  },

  getLastActive(): number | null {
    const raw = cookieService.get(COOKIE_KEYS.LAST_ACTIVE);
    return raw ? Number(raw) : null;
  },

  updateMostVisited(path: string): void {
    const pages =
      cookieService.getJSON<{ path: string; count: number }[]>(COOKIE_KEYS.MOST_VISITED) || [];

    const existing = pages.find((p) => p.path === path);
    if (existing) {
      existing.count++;
    } else {
      pages.push({ path, count: 1 });
    }

    // Keep top 10 most visited
    pages.sort((a, b) => b.count - a.count);
    cookieService.setJSON(COOKIE_KEYS.MOST_VISITED, pages.slice(0, 10));
  },
};
