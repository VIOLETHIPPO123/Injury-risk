const API_BASE_URL = "http://localhost:8080";

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

// Fetches every watchlist entry from GET /watchlist.
export function getWatchlist() {
  return request("GET", "/watchlist");
}

// Adds a player via POST /watchlist. The backend assigns the id.
export function addToWatchlist(playerId, note = "") {
  return request("POST", "/watchlist", { playerId, note });
}

// Edits an entry's note via PUT /watchlist/{id}. Only the note is editable.
export function updateWatchlistEntry(id, note) {
  return request("PUT", `/watchlist/${id}`, { note });
}

// Removes an entry via DELETE /watchlist/{id}.
export function removeFromWatchlist(id) {
  return request("DELETE", `/watchlist/${id}`);
}
