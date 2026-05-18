import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { graphqlRequest } from "./graphqlClient";

// ── helpers ────────────────────────────────────────────────────────────────

const SESSION_KEY = "formforge_user";

const mockSession = {
  id: 1,
  username: "alice",
  role: "USER",
  token: "mock.jwt.token",
};

function mockFetchSuccess(data: unknown) {
  return vi.fn().mockResolvedValue({
    ok: true,
    json: () => Promise.resolve({ data }),
  } as Response);
}

// ── graphqlRequest ─────────────────────────────────────────────────────────

describe("graphqlRequest", () => {
  beforeEach(() => {
    // Clear session storage before each test
    if (typeof sessionStorage !== "undefined") {
      sessionStorage.clear();
    }
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    if (typeof sessionStorage !== "undefined") {
      sessionStorage.clear();
    }
  });

  it("includes Authorization Bearer header when session token is present", async () => {
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(mockSession));
    vi.stubGlobal("fetch", mockFetchSuccess({ applications: [] }));

    await graphqlRequest<unknown>("{ applications { id } }");

    const [, options] = (fetch as ReturnType<typeof vi.fn>).mock.calls[0] as [
      string,
      RequestInit,
    ];
    expect((options.headers as Record<string, string>)["Authorization"]).toBe(
      "Bearer mock.jwt.token"
    );
  });

  it("omits Authorization header when no session exists", async () => {
    vi.stubGlobal("fetch", mockFetchSuccess({ ping: true }));

    await graphqlRequest<unknown>("{ ping }");

    const [, options] = (fetch as ReturnType<typeof vi.fn>).mock.calls[0] as [
      string,
      RequestInit,
    ];
    expect((options.headers as Record<string, string>)["Authorization"]).toBeUndefined();
  });

  it("returns the data property on success", async () => {
    vi.stubGlobal("fetch", mockFetchSuccess({ value: 42 }));

    const result = await graphqlRequest<{ value: number }>("{ value }");
    expect(result).toEqual({ value: 42 });
  });

  it("throws the first GraphQL error message on errors", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        json: () =>
          Promise.resolve({
            errors: [{ message: "Unauthorized" }, { message: "Other error" }],
          }),
      } as Response)
    );

    await expect(graphqlRequest("{ secure }")).rejects.toThrow("Unauthorized");
  });

  it("throws when data is absent and no errors", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({}),
      } as Response)
    );

    await expect(graphqlRequest("{ something }")).rejects.toThrow(
      "No data returned from GraphQL"
    );
  });
});
