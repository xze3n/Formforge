const GRAPHQL_URL = "/graphql";

export interface GraphQLResponse<T> {
  data?: T;
  errors?: Array<{
    message: string;
    extensions?: { classification?: string };
  }>;
}

function userHeaders(): Record<string, string> {
  try {
    const raw = sessionStorage.getItem("formforge_user");
    if (!raw) return {};
    const user = JSON.parse(raw);
    return {
      "X-User-Id":   String(user.id),
      "X-User-Name": user.username,
      "X-User-Role": user.role,
    };
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
    headers: { "Content-Type": "application/json", ...userHeaders() },
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
