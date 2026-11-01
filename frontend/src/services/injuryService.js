const API_BASE_URL = "http://localhost:8080";

// Fetches a player's injury history from GET /players/{id}/injuries, newest first.
export async function getInjuries(playerId) {
  const response = await fetch(`${API_BASE_URL}/players/${playerId}/injuries`);
  if (!response.ok) {
    throw new Error(`GET /players/${playerId}/injuries failed with status ${response.status}`);
  }
  return response.json();
}
