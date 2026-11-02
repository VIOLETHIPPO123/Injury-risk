// ACWR (Acute:Chronic Workload Ratio) compares the most recent game's workload
// against the average of the last four. Roughly 1.0 means "business as usual";
// a big spike is the classic soft-tissue injury warning sign.
export function calculateAcwr(snapsLastGame, snapsLast4Games) {
  const chronic = snapsLast4Games / 4;
  if (!chronic) return null;
  return snapsLastGame / chronic;
}

// tier drives both the text color and the card's background tint —
// under-training and slightly-elevated share "medium" because the risk
// doc treats both as cautionary, just from opposite directions.
export function riskLevel(acwr) {
  if (acwr === null) return { label: "No data", tier: "unknown" };
  if (acwr < 0.8) return { label: "Ramping up", tier: "medium" };
  if (acwr <= 1.3) return { label: "Normal workload", tier: "low" };
  if (acwr <= 1.5) return { label: "Slightly elevated", tier: "medium" };
  return { label: "Spiking — high risk", tier: "high" };
}

const TIER_RANK = { low: 0, medium: 1, high: 2 };

// Higher is riskier; null for "unknown". Used with acwrDistance to break ties within a tier.
export function tierRank(risk) {
  return TIER_RANK[risk.tier] ?? null;
}

// How far a workload is from "business as usual" (1.0), in either direction.
export function acwrDistance(acwr) {
  return acwr === null ? null : Math.abs(acwr - 1);
}

// One ACWR per week from a player's history ([{ week, snaps }], oldest first, snaps null on
// a bye week). Each game is scored the way calculateAcwr scores the player page: that game's
// snaps against the average of the last four games, including it. It counts games, not
// weeks, so a bye is skipped instead of counting as 0, and its acwr is null (a gap). Games
// before the player has four have a null acwr too.
export function weeklyAcwr(history) {
  const games = [];
  return history.map(({ week, snaps }) => {
    if (snaps === null) return { week, snaps, acwr: null };
    games.push(snaps);
    if (games.length < 4) return { week, snaps, acwr: null };
    const lastFour = games.slice(-4).reduce((sum, s) => sum + s, 0);
    return { week, snaps, acwr: calculateAcwr(snaps, lastFour) };
  });
}
