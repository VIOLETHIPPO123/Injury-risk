const API_BASE_URL = "http://localhost:8080";

// Fetches a player's games from GET /players/{id}/workload: [{ playerId, week, snaps }] in
// week order. A bye week has no entry.
export async function getWorkload(playerId) {
  const response = await fetch(`${API_BASE_URL}/players/${playerId}/workload`);
  if (!response.ok) {
    throw new Error(`GET /players/${playerId}/workload failed with status ${response.status}`);
  }
  return response.json();
}
