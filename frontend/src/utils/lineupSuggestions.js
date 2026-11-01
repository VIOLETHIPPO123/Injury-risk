import { calculateAcwr, riskLevel } from "./acwr";
import { BENCH, isStarterSlot } from "./rosterFormats";
import { compareRisk, sortByRisk } from "./sortByRisk";

const acwrOf = (player) => calculateAcwr(player.snapsLastGame, player.snapsLast4Games);

// For each high-risk starter, the lowest-risk bench player who is allowed in that starter's
// slot and is actually less risky by DEV-89's ranking (compareRisk). Each bench player is
// suggested at most once. `players` each carry the `slot` they fill.
// Returns [{ starter, benchPlayer, text }], in the order the starters were given.
export function benchSuggestions(players) {
  const bench = players.filter((p) => p.slot === BENCH && acwrOf(p) !== null);
  const suggested = new Set();

  return players
    .filter((p) => isStarterSlot(p.slot) && riskLevel(acwrOf(p)).tier === "high")
    .flatMap((starter) => {
      const safer = bench.filter(
        (b) =>
          !suggested.has(b.id) &&
          starter.slot.allowed.includes(b.position) &&
          compareRisk(acwrOf(b), acwrOf(starter)) < 0,
      );
      if (safer.length === 0) return [];

      const [benchPlayer] = sortByRisk(safer, "asc");
      suggested.add(benchPlayer.id);
      return [
        {
          starter,
          benchPlayer,
          text: `Consider starting ${benchPlayer.name} over ${starter.name}.`,
        },
      ];
    });
}
