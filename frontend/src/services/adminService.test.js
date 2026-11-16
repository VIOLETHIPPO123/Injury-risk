import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { refreshPlayerData } from "./adminService";
import { SignedOutError } from "./apiFetch";

function respondWith(status, body) {
  fetch.mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    json: body === undefined ? () => Promise.reject(new Error("no body")) : () => Promise.resolve(body),
  });
}

beforeEach(() => {
  localStorage.clear();
  localStorage.setItem(
    "sidelined.session",
    JSON.stringify({ accessToken: "abc123", expiresAt: Math.floor(Date.now() / 1000) + 3600 }),
  );
  vi.stubGlobal("fetch", vi.fn());
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("refreshPlayerData", () => {
  test("POSTs to the refresh endpoint with the stored token and returns what the jobs did", async () => {
    const result = {
      players: { added: 3, updated: 12, skipped: 1, defensesAdded: 0 },
      workloads: { added: 1400, updated: 2, unmatched: 60 },
    };
    respondWith(200, result);

    await expect(refreshPlayerData()).resolves.toEqual(result);
    expect(fetch).toHaveBeenCalledWith("http://localhost:8080/admin/players/refresh", {
      method: "POST",
      headers: { Authorization: "Bearer abc123" },
    });
  });

  test("a non-admin is rejected with the backend's reason", async () => {
    respondWith(403, { error: "Admins only" });

    await expect(refreshPlayerData()).rejects.toThrow("Admins only");
  });

  test("a failure with no JSON body is rejected with the status", async () => {
    respondWith(500);

    await expect(refreshPlayerData()).rejects.toThrow("Refresh failed with status 500");
  });

  test("an expired token signs the user out", async () => {
    respondWith(401, {});

    await expect(refreshPlayerData()).rejects.toBeInstanceOf(SignedOutError);
    expect(localStorage.getItem("sidelined.session")).toBeNull();
  });
});
