function createFormation(teamPlayers) {
  const byPosition = {};

  for (const player of teamPlayers) {
    if (!byPosition[player.position]) {
      byPosition[player.position] = [];
    }

    byPosition[player.position].push(player);
  }

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

export { createFormation };
