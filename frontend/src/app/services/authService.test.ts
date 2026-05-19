import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { loginApi, registerApi, type AuthUser } from "./authService";

// ── helpers ────────────────────────────────────────────────────────────────

const mockUser: AuthUser = {
  id: 1,
  username: "alice",
  email: "alice@test.com",
  role: "USER",
  permissions: ["READ_APPLICATIONS", "WRITE_APPLICATIONS"],
  token: "header.payload.signature",
  refreshToken: "refresh.token.value",
};

function mockFetch(status: number, body: unknown) {
  return vi.fn().mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
  } as Response);
}

// ── loginApi ───────────────────────────────────────────────────────────────

describe("loginApi", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", mockFetch(200, mockUser));
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("sends POST to /api/auth/login with credentials", async () => {
    await loginApi({ identifier: "alice@test.com", password: "secret" });

    expect(fetch).toHaveBeenCalledWith("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ identifier: "alice@test.com", password: "secret" }),
    });
  });

  it("returns the parsed AuthUser including token on success", async () => {
    const result = await loginApi({ identifier: "alice@test.com", password: "secret" });

    expect(result.id).toBe(1);
    expect(result.username).toBe("alice");
    expect(result.role).toBe("USER");
    expect(result.token).toBe("header.payload.signature");
    expect(result.permissions).toContain("READ_APPLICATIONS");
  });

  it("throws with server message on 401", async () => {
    vi.stubGlobal("fetch", mockFetch(401, { message: "Invalid email or password" }));

    await expect(
      loginApi({ identifier: "bad@test.com", password: "wrong" })
    ).rejects.toThrow("Invalid email or password");
  });

  it("throws a fallback message when server body has no message field", async () => {
    vi.stubGlobal("fetch", mockFetch(500, {}));

    await expect(
      loginApi({ identifier: "a@b.com", password: "x" })
    ).rejects.toThrow("Invalid credentials");
  });
});

// ── registerApi ───────────────────────────────────────────────────────────

describe("registerApi", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", mockFetch(201, { ...mockUser, id: 2, username: "bob" }));
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("sends POST to /api/auth/register with credentials", async () => {
    await registerApi({ username: "bob", email: "bob@test.com", password: "pass1234" });

    expect(fetch).toHaveBeenCalledWith("/api/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username: "bob", email: "bob@test.com", password: "pass1234" }),
    });
  });

  it("returns the parsed AuthUser with token on success", async () => {
    const result = await registerApi({
      username: "bob",
      email: "bob@test.com",
      password: "pass1234",
    });

    expect(result.id).toBe(2);
    expect(result.username).toBe("bob");
    expect(typeof result.token).toBe("string");
    expect(result.token.length).toBeGreaterThan(0);
  });

  it("throws with server message on 409 conflict", async () => {
    vi.stubGlobal("fetch", mockFetch(409, { message: "Email already in use" }));

    await expect(
      registerApi({ username: "dup", email: "dup@test.com", password: "pass" })
    ).rejects.toThrow("Email already in use");
  });

  it("throws fallback message when body is not JSON", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: false,
      status: 500,
      json: () => Promise.reject(new Error("not json")),
    } as unknown as Response));

    await expect(
      registerApi({ username: "x", email: "x@x.com", password: "pass" })
    ).rejects.toThrow("Registration failed");
  });
});
