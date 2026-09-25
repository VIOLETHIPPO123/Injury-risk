import { afterEach, beforeEach, expect, test, vi } from "vitest";
import {
  addToWatchlist,
  getWatchlist,
  removeFromWatchlist,
  updateWatchlistEntry,
} from "./watchlistService";

// Stands in for the backend: each test says what the "server" answers.
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

test("addToWatchlist POSTs { playerId, note } as JSON to /watchlist", async () => {
  // Arrange
  mockFetchResponse(201, { id: 1, playerId: 7, note: "" });

  // Act
  const entry = await addToWatchlist(7);

  // Assert
  expect(fetch).toHaveBeenCalledTimes(1);
  const [url, options] = fetch.mock.calls[0];
  expect(url).toBe("http://localhost:8080/watchlist");
  expect(options.method).toBe("POST");
  expect(options.headers).toEqual({ "Content-Type": "application/json" });
  expect(JSON.parse(options.body)).toEqual({ playerId: 7, note: "" });
  expect(entry).toEqual({ id: 1, playerId: 7, note: "" });
});

test("addToWatchlist includes a note when one is given", async () => {
  // Arrange
  mockFetchResponse(201, { id: 2, playerId: 7, note: "Monitor snaps" });

  // Act
  await addToWatchlist(7, "Monitor snaps");

  // Assert
  expect(JSON.parse(fetch.mock.calls[0][1].body)).toEqual({
    playerId: 7,
    note: "Monitor snaps",
  });
});

test("getWatchlist GETs /watchlist and returns the entries", async () => {
  // Arrange
  const entries = [{ id: 1, playerId: 7, note: "" }];
  mockFetchResponse(200, entries);

  // Act
  const result = await getWatchlist();

  // Assert
  expect(fetch).toHaveBeenCalledWith("http://localhost:8080/watchlist", {
    method: "GET",
  });
  expect(result).toEqual(entries);
});

test("updateWatchlistEntry PUTs only the note to /watchlist/{id}", async () => {
  // Arrange
  mockFetchResponse(200, { id: 3, playerId: 7, note: "Snap share dropping" });

  // Act
  await updateWatchlistEntry(3, "Snap share dropping");

  // Assert
  const [url, options] = fetch.mock.calls[0];
  expect(url).toBe("http://localhost:8080/watchlist/3");
  expect(options.method).toBe("PUT");
  expect(JSON.parse(options.body)).toEqual({ note: "Snap share dropping" });
});

test("removeFromWatchlist DELETEs /watchlist/{id} without parsing the 204 body", async () => {
  // Arrange
  mockFetchResponse(204);

  // Act
  const result = await removeFromWatchlist(3);

  // Assert
  expect(fetch).toHaveBeenCalledWith("http://localhost:8080/watchlist/3", {
    method: "DELETE",
  });
  expect(result).toBeNull();
});

test("a failed request throws with the method, path and status", async () => {
  // Arrange
  mockFetchResponse(404, { message: "Player not found" });

  // Act + Assert
  await expect(addToWatchlist(9999)).rejects.toThrow(
    "POST /watchlist failed with status 404",
  );
});
