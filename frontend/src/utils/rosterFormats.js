// Fantasy roster formats come from the backend (GET /roster/formats). toLineupFormat turns
// one into what the lineup needs: one starting slot per spot (each listing the positions
// allowed in it), then how many bench and IR spots there are.

// The player data's positions for each roster position: kickers are "PK" and fullbacks
// "FB" (who play as running backs). DEF is a whole team's unit, so no player fills it.
const PLAYER_POSITIONS = {
  QB: ["QB"],
  RB: ["RB", "FB"],
  WR: ["WR"],
  TE: ["TE"],
  K: ["PK"],
  DEF: ["D/ST"],
};

// `slot` is the backend's name for where the player is (a RosterEntry's slot): both RB
// spots are slot "RB".
const spot = (id, slot, label, allowed) => ({ id, slot, label, allowed });

// A slot with a count of 2 becomes two spots, e.g. RB1 and RB2; a count of 1 keeps its name.
export function toLineupFormat(format) {
  const starters = format.slots.flatMap((s) =>
    Array.from({ length: s.count }, (_, i) =>
      spot(
        s.count > 1 ? `${s.name}${i + 1}` : s.name,
        s.name,
        s.name,
        s.positions.flatMap((p) => PLAYER_POSITIONS[p] ?? []),
      ),
    ),
  );
  return {
    id: format.id,
    label: format.name,
    starters,
    bench: format.benchCount,
    ir: format.irCount,
    limit: format.maxRosterSize,
  };
}

// The backend's default too, so the two agree before the settings have loaded.
export const DEFAULT_FORMAT = "espn-standard";

// Positions a player can be rostered at.
const ELIGIBLE_POSITIONS = new Set(
  ["QB", "RB", "WR", "TE", "K"].flatMap((p) => PLAYER_POSITIONS[p]),
);

export function isRosterEligible(position) {
  return ELIGIBLE_POSITIONS.has(position);
}

// Starters and bench; IR spots are on top of it.
export function rosterLimit(format) {
  return format.limit;
}

// The bench and IR take any rostered player. The bench holds `format.bench` players and
// IR holds `format.ir`; every player on either shares the one slot object.
export const BENCH = spot("BN", "BENCH", "Bench");
export const IR = spot("IR", "IR", "IR");

export function isStarterSlot(slot) {
  return slot !== BENCH && slot !== IR;
}

// Whether `position` may be moved into the slot with this id. IR is limited to
// `format.ir` players, so `irFull` says whether it already is (by someone else).
export function canMoveTo(format, slotId, position, { irFull = false } = {}) {
  if (slotId === BENCH.id) return true;
  if (slotId === IR.id) return !irFull;
  const slot = format.starters.find((s) => s.id === slotId);
  return Boolean(slot?.allowed.includes(position));
}

// Puts each player where the backend has them. `entries` are roster entries
// ({ playerId, slot }) in the order they were added; players sharing a slot fill its
// spots in that order (the first RB gets RB1). Returns a Map of player id -> spot.
export function placeEntries(format, entries) {
  const open = [...format.starters];
  const spots = new Map();

  for (const entry of entries) {
    if (entry.slot === IR.slot) {
      spots.set(entry.playerId, IR);
    } else {
      // A slot this format doesn't have (or no slot yet) means the bench.
      const index = open.findIndex((s) => s.slot === entry.slot);
      spots.set(entry.playerId, index === -1 ? BENCH : open.splice(index, 1)[0]);
    }
  }
  return spots;
}
