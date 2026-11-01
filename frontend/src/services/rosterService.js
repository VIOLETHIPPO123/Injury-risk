const API_BASE_URL = "http://localhost:8080";
// The backend still serves the roster at its original path.
const ROSTER_PATH = "/watchlist";

async function request(method, path, body) {
  const options = { method };
  if (body !== undefined) {
    options.headers = { "Content-Type": "application/json" };
    options.body = JSON.stringify(body);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, options);
  if (!response.ok) {
    throw new Error(`${method} ${path} failed with status ${response.status}`);
  }
  // DELETE answers 204 No Content, so there is no body to parse.
  return response.status === 204 ? null : response.json();
}

// Fetches every roster entry.
export function getRoster() {
  return request("GET", ROSTER_PATH);
}

// Adds a player to the roster. The backend assigns the id.
export function addToRoster(playerId, note = "") {
  return request("POST", ROSTER_PATH, { playerId, note });
}

// Edits an entry's note. Only the note is editable.
export function updateRosterEntry(id, note) {
  return request("PUT", `${ROSTER_PATH}/${id}`, { note });
}

// Removes an entry from the roster.
export function removeFromRoster(id) {
  return request("DELETE", `${ROSTER_PATH}/${id}`);
}
