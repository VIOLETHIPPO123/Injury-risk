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

// The bench and IR take any rostered player. The bench holds `format.bench` players and
// IR holds `format.ir`; every player on either shares the one slot object.
export const BENCH = { id: "BN", label: "Bench" };
export const IR = { id: "IR", label: "IR" };

export function isStarterSlot(slot) {
  return slot !== BENCH && slot !== IR;
}

// Whether `position` may be moved into the slot with this id. IR is limited to
// `format.ir` players, so `irFull` says whether it already is (by someone else).
export function canMoveTo(formatId, slotId, position, { irFull = false } = {}) {
  if (slotId === BENCH.id) return true;
  if (slotId === IR.id) return !irFull;
  const slot = ROSTER_FORMATS[formatId].starters.find((s) => s.id === slotId);
  return Boolean(slot?.allowed.includes(position));
}

// Gives each player a slot. `assignments` (player id -> slot id) holds the moves the user
// made; any that still fit the format are kept. Everyone else, in the order given, gets
// the first open starting slot their position is allowed in, otherwise the bench.
// Returns a Map of player id -> slot.
export function assignSlots(formatId, players, assignments = {}) {
  const format = ROSTER_FORMATS[formatId];
  const open = [...format.starters];
  const slots = new Map();
  let irCount = 0;

  for (const player of players) {
    const slotId = assignments[player.id];
    if (slotId === BENCH.id) {
      slots.set(player.id, BENCH);
    } else if (slotId === IR.id && irCount < format.ir) {
      slots.set(player.id, IR);
      irCount++;
    } else {
      const index = open.findIndex((s) => s.id === slotId && s.allowed.includes(player.position));
      if (index !== -1) slots.set(player.id, open.splice(index, 1)[0]);
    }
  }

  for (const player of players) {
    if (slots.has(player.id)) continue;
    const index = open.findIndex((s) => s.allowed.includes(player.position));
    slots.set(player.id, index === -1 ? BENCH : open.splice(index, 1)[0]);
  }
  return slots;
}
