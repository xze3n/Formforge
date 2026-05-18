const GRAPHQL_URL = "/graphql";

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
    const raw = sessionStorage.getItem("formforge_user");
    if (!raw) return {};
    const user = JSON.parse(raw);
    if (!user?.token) return {};
    return { Authorization: `Bearer ${user.token}` };
  } catch {
    return {};
  }
}

export async function graphqlRequest<T>(
  query: string,
  variables?: Record<string, unknown>
): Promise<T> {
  const response = await fetch(GRAPHQL_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify({ query, variables }),
  });

  const result: GraphQLResponse<T> = await response.json();

  if (result.errors && result.errors.length > 0) {
    throw new Error(result.errors[0].message);
  }

  if (!result.data) {
    throw new Error("No data returned from GraphQL");
  }

  return result.data;
}
