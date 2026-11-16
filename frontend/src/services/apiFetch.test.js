import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { apiFetch, setSignedOutHandler, SignedOutError } from "./apiFetch";
import { loadSession, saveSession } from "./session";

const inOneHour = () => Math.floor(Date.now() / 1000) + 3600;

function respondWith(status) {
  fetch.mockResolvedValue({ ok: status >= 200 && status < 300, status, json: () => Promise.resolve({}) });
}

beforeEach(() => {
  localStorage.clear();
  vi.stubGlobal("fetch", vi.fn());
  respondWith(200);
});

afterEach(() => {
  vi.unstubAllGlobals();
  setSignedOutHandler(null);
});

describe("apiFetch", () => {
  test("adds the signed-in user's token as a Bearer header", async () => {
    saveSession({ access_token: "abc123", expires_at: inOneHour() });

    await apiFetch("/roster");

    expect(fetch).toHaveBeenCalledWith("http://localhost:8080/roster", {
      headers: { Authorization: "Bearer abc123" },
    });
  });

  test("keeps the caller's headers and options alongside the token", async () => {
    saveSession({ access_token: "abc123", expires_at: inOneHour() });

    await apiFetch("/roster", { method: "POST", headers: { "Content-Type": "application/json" }, body: "{}" });

    expect(fetch).toHaveBeenCalledWith("http://localhost:8080/roster", {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: "Bearer abc123" },
      body: "{}",
    });
  });

  test("sends no Authorization header when signed out", async () => {
    await apiFetch("/roster/formats", { method: "GET" });

    expect(fetch).toHaveBeenCalledWith("http://localhost:8080/roster/formats", { method: "GET" });
  });

  test("doesn't send a stored token that has already expired", async () => {
    saveSession({ access_token: "old", expires_at: Math.floor(Date.now() / 1000) - 1 });

    await apiFetch("/roster");

    expect(fetch.mock.calls[0][1].headers).toBeUndefined();
  });

  test("a 401 clears the token, calls the signed-out handler and throws SignedOutError", async () => {
    saveSession({ access_token: "revoked", expires_at: inOneHour() });
    const onSignedOut = vi.fn();
    setSignedOutHandler(onSignedOut);
    respondWith(401);

    await expect(apiFetch("/roster")).rejects.toBeInstanceOf(SignedOutError);

    expect(onSignedOut).toHaveBeenCalledTimes(1);
    expect(loadSession()).toBeNull();
  });

  test("other errors are returned for the caller to handle, without signing out", async () => {
    saveSession({ access_token: "abc123", expires_at: inOneHour() });
    const onSignedOut = vi.fn();
    setSignedOutHandler(onSignedOut);
    respondWith(409);

    const response = await apiFetch("/roster");

    expect(response.status).toBe(409);
    expect(onSignedOut).not.toHaveBeenCalled();
    expect(loadSession()).not.toBeNull();
  });
});

describe("the stored session", () => {
  test("uses expires_at when Supabase sends it", () => {
    const expiresAt = inOneHour();
    saveSession({ access_token: "abc", expires_at: expiresAt });

    expect(loadSession()).toEqual({ accessToken: "abc", expiresAt });
  });

  test("falls back to now + expires_in", () => {
    saveSession({ access_token: "abc", expires_in: 3600 });

    expect(loadSession().expiresAt).toBeGreaterThan(Date.now() / 1000 + 3500);
  });

  test("ignores anything unreadable in storage", () => {
    localStorage.setItem("sidelined.session", "not json");

    expect(loadSession()).toBeNull();
  });
});
