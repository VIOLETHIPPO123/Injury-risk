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

// The search page's state: a name query plus the selected position and team chips.
export const EMPTY_SEARCH = { query: "", positions: [], teams: [] };

// A category with nothing selected doesn't filter.
function matchesAny(selected, value) {
  return selected.length === 0 || selected.includes(value);
}

// Within a category a player matches any selected value (QB or RB); across categories
// they must match all of them (position and team), as well as the name query.
export function filterPlayers(players, { query = "", positions = [], teams = [] }) {
  return filterPlayersByName(players, query).filter(
    (player) =>
      matchesAny(positions, player.position) && matchesAny(teams, player.team),
  );
}
