import { refreshTokenApi, type AuthUser } from "./authService";

const GRAPHQL_URL = "/graphql";
const SESSION_KEY = "formforge_user";

export interface GraphQLResponse<T> {
  data?: T;
  errors?: Array<{
    message: string;
    extensions?: { classification?: string };
  }>;
}

/** Returns the Authorization header if a valid session exists. */
function authHeaders(): Record<string, string> {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY);
    if (!raw) return {};
    const user = JSON.parse(raw);
    if (!user?.token) return {};
    return { Authorization: `Bearer ${user.token}` };
  } catch {
    return {};
  }
}

/** Returns true when the errors array contains an Unauthorized classification. */
function isUnauthorized<T>(result: GraphQLResponse<T>): boolean {
  return result.errors?.some(
    (e) => e.extensions?.classification === "UNAUTHORIZED" || e.message === "Unauthorized"
  ) ?? false;
}

/** Attempt a silent token refresh. Returns true if successful. */
async function tryRefresh(): Promise<boolean> {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY);
    if (!raw) return false;
    const stored = JSON.parse(raw) as AuthUser;
    if (!stored.refreshToken) return false;

    const refreshed = await refreshTokenApi(stored.refreshToken);
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(refreshed));
    return true;
  } catch {
    return false;
  }
}

export async function graphqlRequest<T>(
  query: string,
  variables?: Record<string, unknown>
): Promise<T> {
  const doRequest = async () =>
    fetch(GRAPHQL_URL, {
      method: "POST",
      headers: { "Content-Type": "application/json", ...authHeaders() },
      body: JSON.stringify({ query, variables }),
    });

  let response = await doRequest();

  // Spring Security returns HTTP 401 before GraphQL even runs (expired/missing JWT).
  // Also handle GraphQL-layer UNAUTHORIZED errors (role checks etc.).
  const needsRefresh =
    response.status === 401 ||
    (response.headers.get("content-type")?.includes("application/json") &&
      await response.clone().json().then(isUnauthorized<T>).catch(() => false));

  if (needsRefresh) {
    const refreshed = await tryRefresh();
    if (refreshed) {
      response = await doRequest();
    } else {
      // Refresh failed — force logout so the user is sent to the login page
      window.dispatchEvent(new Event("ff:auth:expired"));
      throw new Error("Session expired. Please log in again.");
    }
  }

  let result: GraphQLResponse<T>;
  try {
    result = await response.json();
  } catch {
    throw new Error(`Unexpected response from server (HTTP ${response.status})`);
  }

  if (result.errors && result.errors.length > 0) {
    throw new Error(result.errors[0].message);
  }

  if (!result.data) {
    throw new Error("No data returned from GraphQL");
  }

  return result.data;
}
