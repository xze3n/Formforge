export interface CookieOptions {
  /** Max age in seconds */
  maxAge?: number;
  /** Expiry date */
  expires?: Date;
  /** Cookie path */
  path?: string;
  /** SameSite attribute */
  sameSite?: "Strict" | "Lax" | "None";
  /** Secure flag — only sent over HTTPS */
  secure?: boolean;
}

const DEFAULT_OPTIONS: CookieOptions = {
  path: "/",
  sameSite: "Lax",
  secure: window.location.protocol === "https:",
  maxAge: 60 * 60 * 24 * 365, // 1 year
};

function buildCookieString(
  name: string,
  value: string,
  options: CookieOptions
): string {
  const parts = [
    `${encodeURIComponent(name)}=${encodeURIComponent(value)}`,
  ];

  if (options.maxAge != null) {
    parts.push(`max-age=${options.maxAge}`);
  }
  if (options.expires) {
    parts.push(`expires=${options.expires.toUTCString()}`);
  }
  if (options.path) {
    parts.push(`path=${options.path}`);
  }
  if (options.sameSite) {
    parts.push(`SameSite=${options.sameSite}`);
  }
  if (options.secure) {
    parts.push("Secure");
  }

  return parts.join("; ");
}

export const cookieService = {
  get(name: string): string | null {
    const match = document.cookie
      .split("; ")
      .find((row) => row.startsWith(`${encodeURIComponent(name)}=`));

    if (!match) return null;
    return decodeURIComponent(match.split("=")[1]);
  },

  getJSON<T>(name: string): T | null {
    const raw = this.get(name);
    if (raw === null) return null;
    try {
      return JSON.parse(raw) as T;
    } catch {
      return null;
    }
  },

  set(name: string, value: string, options?: CookieOptions): void {
    const merged = { ...DEFAULT_OPTIONS, ...options };
    document.cookie = buildCookieString(name, value, merged);
  },

  setJSON<T>(name: string, value: T, options?: CookieOptions): void {
    this.set(name, JSON.stringify(value), options);
  },

  delete(name: string, path = "/"): void {
    document.cookie = buildCookieString(name, "", {
      path,
      maxAge: 0,
    });
  },

  exists(name: string): boolean {
    return this.get(name) !== null;
  },

  /** Returns all cookie names managed by the app (ff_ prefix). */
  getAllAppCookies(): string[] {
    return document.cookie
      .split("; ")
      .map((row) => decodeURIComponent(row.split("=")[0]))
      .filter((name) => name.startsWith("ff_"));
  },

  /** Delete all app-specific cookies. */
  clearAllAppCookies(): void {
    this.getAllAppCookies().forEach((name) => this.delete(name));
  },
};
