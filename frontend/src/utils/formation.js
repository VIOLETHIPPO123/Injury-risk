// Lays out a unit's players from a formation definition (see formationDefinitions.js).
// A definition is a list of named rows, each a list of slots:
//   { rows: [{ name: "top-row", slots: [Slot.pick(position), ...] }, ...] }
// This file knows nothing about football positions.

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

// Splits a list so the first group sits on the left and the rest on the right,
// which keeps the "interior" players centered (edge, middle, middle, edge)
function centerBetween(edges, middle) {
  const half = Math.ceil(edges.length / 2);
  return [...edges.slice(0, half), ...middle, ...edges.slice(half)];
}

// Strategy: every slot is an object with resolve(byPosition), returning the players it
// fills (possibly none). createFormation calls resolve without knowing which kind it is.
// Factory: these methods are the only place slot strategies are created; definitions
// call them instead of building slot objects by hand.
const Slot = {
  // The nth player at a position (the first by default).
  pick: (position, nth = 0) => ({
    resolve: (byPosition) => [byPosition[position]?.[nth]],
  }),

  // Every player at a position from index `start` on, however many there are.
  from: (position, start = 0) => ({
    resolve: (byPosition) => (byPosition[position] ?? []).slice(start),
  }),

  // The `edges` players split around the `middle` players.
  centered: (edges, middle) => ({
    resolve: (byPosition) =>
      centerBetween(edges.resolve(byPosition), middle.resolve(byPosition)),
  }),
};

// Returns the definition's rows in order, each with the players its slots found.
function createFormation(players, formation) {
  const byPosition = groupByPosition(players);

  return formation.rows.map((row) => ({
    name: row.name,
    players: row.slots
      .flatMap((slot) => slot.resolve(byPosition))
      .filter(Boolean),
  }));
}

export { Slot, createFormation };
