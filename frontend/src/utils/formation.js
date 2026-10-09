function groupByPosition(players) {
  const byPosition = {};

  for (const player of players) {
    if (!byPosition[player.position]) {
      byPosition[player.position] = [];
    }

    byPosition[player.position].push(player);
  }

  return byPosition;
}

function createFormation(teamPlayers) {
  const byPosition = groupByPosition(teamPlayers);

  return {
    topRow: [
      byPosition.LT?.[0],
      byPosition.LG?.[0],
      byPosition.C?.[0],
      byPosition.RG?.[0],
      byPosition.RT?.[0],
      byPosition.TE?.[0],
    ].filter(Boolean),

    bottomRow: [
      byPosition.WR?.[0],
      byPosition.WR?.[1],
      byPosition.RB?.[0],
      byPosition.QB?.[0],
      byPosition.FB?.[0],
      byPosition.WR?.[2],
    ].filter(Boolean),
  };
}

// Defense formation creation
// Splits a list so the first group sits on the left and the rest on the right,
// which keeps the "interior" players centered (e.g. DE, DT, DT, DE)
function centerBetween(edges, middle) {
  const half = Math.ceil(edges.length / 2);
  return [...edges.slice(0, half), ...middle, ...edges.slice(half)];
}

function createDefenseFormation(teamPlayers) {
  const byPosition = groupByPosition(teamPlayers);

  const ends = byPosition.DE ?? [];
  const tackles = byPosition.DT ?? [];
  const linebackers = byPosition.LB ?? [];
  const corners = byPosition.CB ?? [];
  const strongSafeties = byPosition.SS?.[0];
  const freeSafeties = byPosition.FS?.[0];

  // Correctly position nickelback: CB1 CB3 (nickelback) FS SS CB2
  const [cb1, cb2, ...nickelCorners] = corners;

  return {
    // Secondary: CB CB SS FS CB
    topRow: [cb1, ...nickelCorners, freeSafeties, strongSafeties, cb2].filter(
      Boolean,
    ),

    // Linebackers: 3 (4-3) or 4 (3-4)
    middleRow: linebackers,

    // Defensive line: DE DT DT DE (4-3) or DE DT DE (3-4)
    bottomRow: centerBetween(ends, tackles),
  };
}

// Special Teams formation creation
// Outputs one row of 6 players: PK, P, H, PR, KR, LS
function createSpecialTeamsFormation(teamPlayers) {
  const byPosition = groupByPosition(teamPlayers);

  return {
    topRow: [
      byPosition.PK?.[0],
      byPosition.P?.[0],
      byPosition.H?.[0],
      byPosition.PR?.[0],
      byPosition.KR?.[0],
      byPosition.LS?.[0],
    ].filter(Boolean),
  };
}

export { createFormation, createDefenseFormation, createSpecialTeamsFormation };
