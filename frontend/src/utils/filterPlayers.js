// Strips everything except letters and digits, then lowercases.
// This makes matching insensitive to case, apostrophes, hyphens,
// periods, extra spaces, etc.
function normalize(str) {
  return str.toLowerCase().replace(/[^a-z0-9]/gi, "");
}

// Returns only players whose name contains the query as a substring,
// once both are normalized. Empty/whitespace-only query returns all players.
export function filterPlayersByName(players, query) {
  const normalizedQuery = normalize(query);

  if (!normalizedQuery) return players;

  return players.filter((player) =>
    normalize(player.name).includes(normalizedQuery),
  );
}
