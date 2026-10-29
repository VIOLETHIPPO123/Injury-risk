import { acwrDistance, calculateAcwr, riskLevel, tierRank } from "./acwr";

// The positions in a team's D/ST unit: the defense plus special teams.
export const DST_POSITIONS = ["DE", "DT", "LB", "CB", "FS", "SS", "PK", "P", "H", "PR", "KR", "LS"];

// A player's ACWR from their snaps, or the ACWR a D/ST unit already carries.
function acwrOf(entry) {
  return "acwr" in entry ? entry.acwr : calculateAcwr(entry.snapsLastGame, entry.snapsLast4Games);
}

// The risk rule, kept in one place: a higher tier is riskier, and within a tier the ACWR
// further from 1.0 is. Positive when the first ACWR is riskier, negative when the second
// is, 0 when they are level. Both ACWRs must be known; callers decide where "no data" goes.
export function compareRisk(firstAcwr, secondAcwr) {
  return (
    tierRank(riskLevel(firstAcwr)) - tierRank(riskLevel(secondAcwr)) ||
    acwrDistance(firstAcwr) - acwrDistance(secondAcwr)
  );
}

// A team's D/ST as one entry that ranks like a player, e.g. "ARI D/ST". Its ACWR is the
// average ACWR of the team's defensive and special teams players. Players with no ACWR
// are left out of the average; if none has one, the unit has no ACWR either.
export function teamDefense(players, team) {
  const acwrs = players
    .filter((p) => p.team === team && DST_POSITIONS.includes(p.position))
    .map(acwrOf)
    .filter((acwr) => acwr !== null);
  const acwr = acwrs.length ? acwrs.reduce((sum, a) => sum + a, 0) / acwrs.length : null;

  return { name: `${team} D/ST`, team, position: "D/ST", acwr };
}

// Ranks players (or D/ST units) by compareRisk. "desc" puts the highest risk first, "asc"
// the lowest. Equal risk goes A to Z by name in both directions, and entries with no ACWR
// always go last, also A to Z. Returns a new list.
export function sortByRisk(entries, direction = "desc") {
  const sign = direction === "asc" ? 1 : -1;
  const byName = (a, b) => a.entry.name.localeCompare(b.entry.name);
  const ranked = entries.map((entry) => ({ entry, acwr: acwrOf(entry) }));

  const known = ranked
    .filter((r) => r.acwr !== null)
    .sort((a, b) => sign * compareRisk(a.acwr, b.acwr) || byName(a, b));
  const noData = ranked.filter((r) => r.acwr === null).sort(byName);

  return [...known, ...noData].map((r) => r.entry);
}
