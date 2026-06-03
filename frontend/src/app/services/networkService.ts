type NetworkListener = (online: boolean) => void;

const API_BASE = import.meta.env.VITE_API_URL ?? "";
const PING_URL = `${API_BASE}/api/auth/ping`;
const PING_INTERVAL = 10_000;

let listeners: NetworkListener[] = [];
let isOnline = navigator.onLine;
let pingTimer: ReturnType<typeof setInterval> | null = null;

async function checkServer(): Promise<boolean> {
  try {
    const res = await fetch(PING_URL, { method: "GET", cache: "no-store" });
    return res.ok;
  } catch {
    return false;
  }
}

function setOnline(value: boolean) {
  if (value === isOnline) return;
  isOnline = value;
  listeners.forEach((fn) => fn(value));
}

async function evaluate() {
  if (!navigator.onLine) {
    setOnline(false);
    return;
  }
  const reachable = await checkServer();
  setOnline(reachable);
}

function handleBrowserOnline() {
  evaluate();
}

function handleBrowserOffline() {
  setOnline(false);
}

export const networkService = {
  /** Current connectivity state */
  get online() {
    return isOnline;
  },

  /** Subscribe to connectivity changes. Returns unsubscribe function. */
  subscribe(fn: NetworkListener): () => void {
    listeners.push(fn);
    return () => {
      listeners = listeners.filter((l) => l !== fn);
    };
  },

  /** Start monitoring (call once at app boot) */
  start() {
    window.addEventListener("online", handleBrowserOnline);
    window.addEventListener("offline", handleBrowserOffline);
    pingTimer = setInterval(evaluate, PING_INTERVAL);
    evaluate();
  },

  /** Stop monitoring */
  stop() {
    window.removeEventListener("online", handleBrowserOnline);
    window.removeEventListener("offline", handleBrowserOffline);
    if (pingTimer) {
      clearInterval(pingTimer);
      pingTimer = null;
    }
  },

  /** Force a re-evaluation now */
  check() {
    return evaluate();
  },
};
