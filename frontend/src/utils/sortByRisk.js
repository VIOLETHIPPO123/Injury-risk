import { acwrDistance, calculateAcwr, riskLevel, tierRank } from "./acwr";

// Ranks players the same way the compare panel's verdict does: a higher tier is riskier, and
// within a tier the ACWR further from 1.0 is. "desc" puts the highest risk first, "asc" the
// lowest. Players with no data always go last, and ties are broken by name so the order is stable.
export function sortByRisk(players, direction = "desc") {
  const sign = direction === "asc" ? -1 : 1;
  const ranked = players.map((player) => {
    const acwr = calculateAcwr(player.snapsLastGame, player.snapsLast4Games);
    return { player, tier: tierRank(riskLevel(acwr)), distance: acwrDistance(acwr) };
  });

  return ranked
    .sort((a, b) => {
      if ((a.tier === null) !== (b.tier === null)) return a.tier === null ? 1 : -1;
      const byRisk = a.tier === null ? 0 : b.tier - a.tier || b.distance - a.distance;
      return sign * byRisk || a.player.name.localeCompare(b.player.name);
    })
    .map(({ player }) => player);
}
