import { afterEach, beforeEach, expect, test, vi } from "vitest";
import {
  addToRoster,
  getRoster,
  getRosterFormats,
  getRosterSettings,
  moveRosterEntry,
  removeFromRoster,
  updateRosterEntry,
  updateRosterSettings,
} from "./rosterService";

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

test("addToRoster POSTs { playerId, note } as JSON to /roster", async () => {
  // Arrange
  mockFetchResponse(201, { id: 1, playerId: 7, note: "" });

  // Act
  const entry = await addToRoster(7);

  // Assert
  expect(fetch).toHaveBeenCalledTimes(1);
  const [url, options] = fetch.mock.calls[0];
  expect(url).toBe("http://localhost:8080/roster");
  expect(options.method).toBe("POST");
  expect(options.headers).toEqual({ "Content-Type": "application/json" });
  expect(JSON.parse(options.body)).toEqual({ playerId: 7, note: "" });
  expect(entry).toEqual({ id: 1, playerId: 7, note: "" });
});

test("addToRoster includes a note when one is given", async () => {
  // Arrange
  mockFetchResponse(201, { id: 2, playerId: 7, note: "Monitor snaps" });

  // Act
  await addToRoster(7, "Monitor snaps");

  // Assert
  expect(JSON.parse(fetch.mock.calls[0][1].body)).toEqual({
    playerId: 7,
    note: "Monitor snaps",
  });
});

test("getRoster GETs /roster and returns the entries", async () => {
  // Arrange
  const entries = [{ id: 1, playerId: 7, note: "" }];
  mockFetchResponse(200, entries);

  // Act
  const result = await getRoster();

  // Assert
  expect(fetch).toHaveBeenCalledWith("http://localhost:8080/roster", {
    method: "GET",
  });
  expect(result).toEqual(entries);
});

test("updateRosterEntry PUTs only the note to /roster/{id}", async () => {
  // Arrange
  mockFetchResponse(200, { id: 3, playerId: 7, note: "Snap share dropping" });

  // Act
  await updateRosterEntry(3, "Snap share dropping");

  // Assert
  const [url, options] = fetch.mock.calls[0];
  expect(url).toBe("http://localhost:8080/roster/3");
  expect(options.method).toBe("PUT");
  expect(JSON.parse(options.body)).toEqual({ note: "Snap share dropping" });
});

test("removeFromRoster DELETEs /roster/{id} without parsing the 204 body", async () => {
  // Arrange
  mockFetchResponse(204);

  // Act
  const result = await removeFromRoster(3);

  // Assert
  expect(fetch).toHaveBeenCalledWith("http://localhost:8080/roster/3", {
    method: "DELETE",
  });
  expect(result).toBeNull();
});

test("a failed request throws with the method, path and status", async () => {
  // Arrange
  mockFetchResponse(404, { message: "Player not found" });

  // Act + Assert
  await expect(addToRoster(9999)).rejects.toThrow(
    "POST /roster failed with status 404",
  );
});

test("getRosterFormats GETs /roster/formats", async () => {
  // Arrange
  const formats = [{ id: "espn-standard", name: "ESPN Standard" }];
  mockFetchResponse(200, formats);

  // Act
  const result = await getRosterFormats();

  // Assert
  expect(fetch).toHaveBeenCalledWith("http://localhost:8080/roster/formats", { method: "GET" });
  expect(result).toEqual(formats);
});

test("getRosterSettings GETs /roster/settings", async () => {
  // Arrange
  mockFetchResponse(200, { formatId: "espn-standard" });

  // Act
  const result = await getRosterSettings();

  // Assert
  expect(fetch).toHaveBeenCalledWith("http://localhost:8080/roster/settings", { method: "GET" });
  expect(result).toEqual({ formatId: "espn-standard" });
});

test("updateRosterSettings PUTs { formatId } to /roster/settings", async () => {
  // Arrange
  mockFetchResponse(200, { formatId: "sleeper-default" });

  // Act
  await updateRosterSettings("sleeper-default");

  // Assert
  const [url, options] = fetch.mock.calls[0];
  expect(url).toBe("http://localhost:8080/roster/settings");
  expect(options.method).toBe("PUT");
  expect(JSON.parse(options.body)).toEqual({ formatId: "sleeper-default" });
});

test("moveRosterEntry PUTs { slot } to /roster/{id}/slot", async () => {
  // Arrange
  mockFetchResponse(200, { id: 3, playerId: 7, slot: "QB", note: "" });

  // Act
  const entry = await moveRosterEntry(3, "QB");

  // Assert
  const [url, options] = fetch.mock.calls[0];
  expect(url).toBe("http://localhost:8080/roster/3/slot");
  expect(options.method).toBe("PUT");
  expect(JSON.parse(options.body)).toEqual({ slot: "QB" });
  expect(entry.slot).toBe("QB");
});

test("a rejected request includes the backend's reason", async () => {
  // Arrange
  mockFetchResponse(409, { error: "No open spot in QB" });

  // Act + Assert
  await expect(moveRosterEntry(3, "QB")).rejects.toThrow(
    "PUT /roster/3/slot failed with status 409: No open spot in QB",
  );
});
