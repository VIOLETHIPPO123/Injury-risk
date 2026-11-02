// Fantasy roster formats: the starting slots (each listing the positions allowed in it),
// then how many bench and IR spots there are. The roster limit is starters + bench.
// The data calls kickers "PK" and fullbacks "FB" (who play as running backs).
const QB = ["QB"];
const RB = ["RB", "FB"];
const WR = ["WR"];
const TE = ["TE"];
const K = ["PK"];
const DST = ["D/ST"];
const FLEX = [...RB, ...WR, ...TE];
const SUPERFLEX = [...QB, ...FLEX];

const slot = (id, label, allowed) => ({ id, label, allowed });

export const ROSTER_FORMATS = {
  standard: {
    label: "Standard",
    starters: [
      slot("QB", "QB", QB),
      slot("RB1", "RB", RB),
      slot("RB2", "RB", RB),
      slot("WR1", "WR", WR),
      slot("WR2", "WR", WR),
      slot("TE", "TE", TE),
      slot("FLEX", "FLEX", FLEX),
      slot("K", "K", K),
      slot("DST", "D/ST", DST),
    ],
    bench: 7,
    ir: 1,
  },
  superflex: {
    label: "Superflex",
    starters: [
      slot("QB", "QB", QB),
      slot("RB1", "RB", RB),
      slot("RB2", "RB", RB),
      slot("WR1", "WR", WR),
      slot("WR2", "WR", WR),
      slot("TE", "TE", TE),
      slot("FLEX", "FLEX", FLEX),
      slot("SFLEX", "SUPERFLEX", SUPERFLEX),
      slot("K", "K", K),
      slot("DST", "D/ST", DST),
    ],
    bench: 6,
    ir: 1,
  },
  "two-qb": {
    label: "2-QB",
    starters: [
      slot("QB1", "QB", QB),
      slot("QB2", "QB", QB),
      slot("RB1", "RB", RB),
      slot("RB2", "RB", RB),
      slot("WR1", "WR", WR),
      slot("WR2", "WR", WR),
      slot("TE", "TE", TE),
      slot("FLEX", "FLEX", FLEX),
      slot("K", "K", K),
      slot("DST", "D/ST", DST),
    ],
    bench: 6,
    ir: 1,
  },
};

export const DEFAULT_FORMAT = "standard";

// Positions a player can be rostered at. D/ST is a whole team's unit, not a player.
const ELIGIBLE_POSITIONS = new Set([...SUPERFLEX, ...K]);

export function isRosterEligible(position) {
  return ELIGIBLE_POSITIONS.has(position);
}

export function rosterLimit(formatId) {
  const format = ROSTER_FORMATS[formatId];
  return format.starters.length + format.bench;
}

export const BENCH = { id: "BN", label: "Bench" };

// Gives each player a slot, in the order given: the first open starting slot their position
// is allowed in, otherwise the bench. Returns a Map of player id -> slot.
export function assignSlots(formatId, players) {
  const open = [...ROSTER_FORMATS[formatId].starters];
  const slots = new Map();

  for (const player of players) {
    const index = open.findIndex((s) => s.allowed.includes(player.position));
    slots.set(player.id, index === -1 ? BENCH : open.splice(index, 1)[0]);
  }
  return slots;
}
