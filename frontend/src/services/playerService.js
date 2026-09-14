const API_BASE_URL = "http://localhost:8080";

// Fetches every player from the Spring Boot GET /players endpoint.
export async function getPlayers() {
  const response = await fetch(`${API_BASE_URL}/players`);
  if (!response.ok) {
    throw new Error(`GET /players failed with status ${response.status}`);
  }
  return response.json();
}
