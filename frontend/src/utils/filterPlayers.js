// Queries shorter than this (after normalizing) are too broad to search on.
export const MIN_QUERY_LENGTH = 2;

const NAME_SUFFIXES = new Set(["jr", "sr", "ii", "iii", "iv", "v"]);

// Strips everything except letters and digits, then lowercases.
// This makes matching insensitive to case, apostrophes, hyphens,
// periods, extra spaces, etc.
function normalize(str) {
  return str.toLowerCase().replace(/[^a-z0-9]/g, "");
}

// Splits on spaces and hyphens and normalizes each piece: "Ja'Marr Chase" → ["jamarr", "chase"].
function words(str) {
  return str.split(/[\s-]+/).map(normalize).filter(Boolean);
}

// The first space-separated part is the first name; the rest, minus a trailing
// suffix like "Jr.", is the last name. "Amon-Ra St. Brown" → first ["amon", "ra"],
// last ["st", "brown"].
function parseName(name) {
  const parts = name.trim().split(/\s+/);
  let lastEnd = parts.length;
  while (lastEnd > 2 && NAME_SUFFIXES.has(normalize(parts[lastEnd - 1]))) lastEnd--;
  const first = words(parts[0]);
  const last = parts.slice(1, lastEnd).flatMap(words);
  // Suffixes still match a query; they just don't rank as the last name.
  const all = [...first, ...last, ...parts.slice(lastEnd).flatMap(words)];
  // Each word joined to the rest of the name, so a query can run across punctuation and
  // spaces: "amonrastbrown", "rastbrown", "stbrown", "brown".
  const starts = all.map((_, i) => all.slice(i).join(""));
  return {
    firstNameStarts: starts.slice(0, first.length),
    lastNameStarts: starts.slice(first.length, first.length + last.length),
    starts,
    lastName: last.join(""),
  };
}

const startsWith = (prefix) => (start) => start.startsWith(prefix);

// True once the query is long enough to search on.
export function hasActiveQuery(query) {
  return normalize(query).length >= MIN_QUERY_LENGTH;
}

// True when the search would show results: a long-enough query or any selected chip.
export function hasActiveSearch({ query = "", positions = [], teams = [] }) {
  return hasActiveQuery(query) || positions.length > 0 || teams.length > 0;
}

// Every word of the query must match the start of a word in the name: "jo al" finds
// Josh Allen, "jamarr" finds Ja'Marr Chase, but "llen" finds nobody.
function matchesName(parsed, queryWords) {
  return queryWords.every((word) => parsed.starts.some(startsWith(word)));
}

// 0 = the query starts the last name, 1 = it starts the first name, 2 = anything else.
function rank(parsed, normalizedQuery) {
  if (parsed.lastNameStarts.some(startsWith(normalizedQuery))) return 0;
  if (parsed.firstNameStarts.some(startsWith(normalizedQuery))) return 1;
  return 2;
}

// Returns players matching the query, last-name matches first, then first-name
// matches, then alphabetically by last name. A query that's too short returns all players.
export function filterPlayersByName(players, query) {
  if (!hasActiveQuery(query)) return players;

  const queryWords = words(query);
  const normalizedQuery = normalize(query);

  return players
    .map((player) => ({ player, parsed: parseName(player.name) }))
    .filter(({ parsed }) => matchesName(parsed, queryWords))
    .map((match) => ({ ...match, rank: rank(match.parsed, normalizedQuery) }))
    .sort(
      (a, b) =>
        a.rank - b.rank ||
        a.parsed.lastName.localeCompare(b.parsed.lastName) ||
        a.player.name.localeCompare(b.player.name),
    )
    .map(({ player }) => player);
}

// The search page's state: a name query plus the selected position and team chips.
export const EMPTY_SEARCH = { query: "", positions: [], teams: [] };

// A category with nothing selected doesn't filter.
function matchesAny(selected, value) {
  return selected.length === 0 || selected.includes(value);
}

// Nothing to search on (no long-enough query and no chips) returns no players.
// Within a category a player matches any selected value (QB or RB); across categories
// they must match all of them (position and team), as well as the name query.
export function filterPlayers(players, search) {
  if (!hasActiveSearch(search)) return [];

  const { query = "", positions = [], teams = [] } = search;
  return filterPlayersByName(players, query).filter(
    (player) =>
      matchesAny(positions, player.position) && matchesAny(teams, player.team),
  );
}
