import { afterEach, beforeEach, expect, test, vi } from "vitest";
import { getInjuries } from "./injuryService";

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

test("getInjuries GETs the player's injuries and returns them as sent", async () => {
  const injuries = [{ season: 2026, week: 4, bodyPart: "Ankle", type: "Sprain", gamesMissed: 1, status: "Recovering" }];
  mockFetchResponse(200, injuries);

  const result = await getInjuries(92);

  expect(fetch).toHaveBeenCalledWith("http://localhost:8080/players/92/injuries");
  expect(result).toEqual(injuries);
});

test("a failed request throws with the path and status", async () => {
  mockFetchResponse(404, { error: "Player not found: 9999" });

  await expect(getInjuries(9999)).rejects.toThrow("GET /players/9999/injuries failed with status 404");
});
