import { apiFetch } from "./apiFetch";

const ROSTER_PATH = "/roster";

// Every roster call is the signed-in user's: apiFetch adds their token, and a missing or
// expired one signs them out (SignedOutError) instead of failing here.
async function request(method, path, body) {
  const options = { method };
  if (body !== undefined) {
    options.headers = { "Content-Type": "application/json" };
    options.body = JSON.stringify(body);
  }

  const response = await apiFetch(path, options);
  if (!response.ok) {
    // The backend explains a rejection in { error }, e.g. why a move isn't allowed.
    let reason;
    try {
      reason = (await response.json())?.error;
    } catch {
      // No JSON body: the status alone will have to do.
    }
    const message = `${method} ${path} failed with status ${response.status}`;
    throw new Error(reason ? `${message}: ${reason}` : message);
  }
  // DELETE answers 204 No Content, so there is no body to parse.
  return response.status === 204 ? null : response.json();
}

// Fetches every roster entry.
export function getRoster() {
  return request("GET", ROSTER_PATH);
}

// Adds a player to the roster. The backend assigns the id and puts them on the bench.
export function addToRoster(playerId, note = "") {
  return request("POST", ROSTER_PATH, { playerId, note });
}

// Edits an entry's note. Moving a player goes through moveRosterEntry.
export function updateRosterEntry(id, note) {
  return request("PUT", `${ROSTER_PATH}/${id}`, { note });
}

// Moves a player to a slot: a starting slot's name (e.g. "RB"), "BENCH" or "IR".
export function moveRosterEntry(id, slot) {
  return request("PUT", `${ROSTER_PATH}/${id}/slot`, { slot });
}

// Removes an entry from the roster.
export function removeFromRoster(id) {
  return request("DELETE", `${ROSTER_PATH}/${id}`);
}

// Fetches every roster format the league can pick.
export function getRosterFormats() {
  return request("GET", "/roster/formats");
}

// Fetches the signed-in user's settings: { formatId }.
export function getRosterSettings() {
  return request("GET", "/roster/settings");
}

// Saves the signed-in user's format. The backend rejects an unknown one with a 400.
export function updateRosterSettings(formatId) {
  return request("PUT", "/roster/settings", { formatId });
}
