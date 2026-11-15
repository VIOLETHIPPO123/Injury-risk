import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { getMyRole, signIn, signUp } from "./authService";
import { loadSession } from "./session";

const inOneHour = () => Math.floor(Date.now() / 1000) + 3600;

function respondWith(status, body) {
  fetch.mockResolvedValue({ ok: status >= 200 && status < 300, status, json: () => Promise.resolve(body) });
}

beforeEach(() => {
  localStorage.clear();
  vi.stubGlobal("fetch", vi.fn());
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("signIn", () => {
  test("POSTs the email and password to our backend and stores the token", async () => {
    respondWith(200, { access_token: "abc123", expires_at: inOneHour(), user: { id: "u1" } });

    await signIn("fan@example.com", "secret");

    expect(fetch).toHaveBeenCalledWith("http://localhost:8080/auth/signin", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email: "fan@example.com", password: "secret" }),
    });
    expect(loadSession().accessToken).toBe("abc123");
  });

  test("shows Supabase's message for a wrong password (current shape)", async () => {
    respondWith(400, { code: 400, error_code: "invalid_credentials", msg: "Invalid login credentials" });

    await expect(signIn("fan@example.com", "wrong")).rejects.toThrow("Invalid login credentials");
    expect(loadSession()).toBeNull();
  });

  test("shows Supabase's message in its older shape", async () => {
    respondWith(400, { error: "invalid_grant", error_description: "Invalid login credentials" });

    await expect(signIn("fan@example.com", "wrong")).rejects.toThrow("Invalid login credentials");
  });

  test("shows our backend's own error when Supabase can't be reached", async () => {
    respondWith(502, { error: "Sign-in service unavailable" });

    await expect(signIn("fan@example.com", "secret")).rejects.toThrow("Sign-in service unavailable");
  });
});

describe("signUp", () => {
  test("signs straight in when Supabase returns a token", async () => {
    respondWith(200, { access_token: "new", expires_in: 3600 });

    await expect(signUp("new@example.com", "secret")).resolves.toEqual({ signedIn: true });
    expect(fetch.mock.calls[0][0]).toBe("http://localhost:8080/auth/signup");
    expect(loadSession().accessToken).toBe("new");
  });

  test("waits for email confirmation when Supabase returns only the user", async () => {
    respondWith(200, { id: "u2", email: "new@example.com" });

    await expect(signUp("new@example.com", "secret")).resolves.toEqual({ signedIn: false });
    expect(loadSession()).toBeNull();
  });

  test("shows why an account couldn't be made", async () => {
    respondWith(422, { code: 422, error_code: "user_already_exists", msg: "User already registered" });

    await expect(signUp("fan@example.com", "secret")).rejects.toThrow("User already registered");
  });
});

describe("getMyRole", () => {
  test("asks /me with the stored token", async () => {
    localStorage.setItem("sidelined.session", JSON.stringify({ accessToken: "abc123", expiresAt: inOneHour() }));
    respondWith(200, { role: "ADMIN" });

    await expect(getMyRole()).resolves.toBe("ADMIN");
    expect(fetch).toHaveBeenCalledWith("http://localhost:8080/me", {
      headers: { Authorization: "Bearer abc123" },
    });
  });
});
