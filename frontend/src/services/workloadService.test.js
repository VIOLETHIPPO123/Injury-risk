import { afterEach, beforeEach, expect, test, vi } from "vitest";
import { getWorkload } from "./workloadService";

function mockFetchResponse(status, body) {
  globalThis.fetch.mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
  });
}

beforeEach(() => {
  vi.stubGlobal("fetch", vi.fn());
});

afterEach(() => {
  vi.unstubAllGlobals();
});

test("getWorkload GETs the player's games and returns them as sent", async () => {
  const weeks = [{ playerId: 1, week: 1, snaps: 60 }, { playerId: 1, week: 3, snaps: 55 }];
  mockFetchResponse(200, weeks);

  const result = await getWorkload(1);

  expect(fetch).toHaveBeenCalledWith("http://localhost:8080/players/1/workload");
  expect(result).toEqual(weeks);
});

test("a failed request throws with the path and status", async () => {
  mockFetchResponse(404, { error: "Player not found: 9999" });

  await expect(getWorkload(9999)).rejects.toThrow("GET /players/9999/workload failed with status 404");
});
