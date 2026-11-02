const API_BASE_URL = "http://localhost:8080";

// Fetches a player's week-by-week snaps from GET /players/{id}/workload, oldest first.
// A bye week comes back as { week, snaps: null }.
export async function getWorkload(playerId) {
  const response = await fetch(`${API_BASE_URL}/players/${playerId}/workload`);
  if (!response.ok) {
    throw new Error(`GET /players/${playerId}/workload failed with status ${response.status}`);
  }
  return response.json();
}
