import { acwrDistance, calculateAcwr, riskLevel, tierRank } from "./acwr";

function stats(player) {
  const acwr = calculateAcwr(player.snapsLastGame, player.snapsLast4Games);
  return {
    acwr,
    risk: riskLevel(acwr),
    lastGame: player.snapsLastGame,
    average: player.snapsLast4Games ? player.snapsLast4Games / 4 : null,
  };
}

// 0 when the first score is riskier, 1 when the second is, null when level or unknown.
function riskierOf(first, second) {
  if (first === null || second === null || first === second) return null;
  return first > second ? 0 : 1;
}

function formatNumber(value) {
  return value === null ? "—" : value.toFixed(2);
}

// The four rows of the side-by-side view. `riskier` is the column (0 or 1) to highlight, or null.
export function comparisonRows(first, second) {
  const a = stats(first);
  const b = stats(second);

  return [
    {
      label: "ACWR",
      values: [formatNumber(a.acwr), formatNumber(b.acwr)],
      riskier: riskierOf(acwrDistance(a.acwr), acwrDistance(b.acwr)),
    },
    {
      label: "Risk tier",
      values: [a.risk.label, b.risk.label],
      riskier: riskierOf(tierRank(a.risk), tierRank(b.risk)),
    },
    {
      label: "Last game snaps",
      values: [String(a.lastGame), String(b.lastGame)],
      riskier: riskierOf(a.lastGame, b.lastGame),
    },
    {
      label: "4-game average",
      values: [formatNumber(a.average), formatNumber(b.average)],
      riskier: riskierOf(a.average, b.average),
    },
  ];
}

// Higher tier is riskier; within a tier, the ACWR further from 1.0 is riskier.
export function compareVerdict(first, second) {
  const a = stats(first);
  const b = stats(second);

  if (a.acwr === null || b.acwr === null) return "Not enough data to call it.";

  const riskier =
    riskierOf(tierRank(a.risk), tierRank(b.risk)) ??
    riskierOf(acwrDistance(a.acwr), acwrDistance(b.acwr));
  if (riskier === null) return "Too close to call.";

  const safer = riskier === 0 ? second : first;
  return `Safer start: ${safer.name}.`;
}
