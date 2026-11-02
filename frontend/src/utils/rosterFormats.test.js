import { describe, expect, it } from "vitest";
import {
  assignSlots,
  BENCH,
  canMoveTo,
  DEFAULT_FORMAT,
  IR,
  isRosterEligible,
  ROSTER_FORMATS,
  rosterLimit,
} from "./rosterFormats";

const player = (id, position) => ({ id, position });
const slotLabels = (formatId, players, assignments = {}) => {
  const slots = assignSlots(formatId, players, assignments);
  return players.map((p) => slots.get(p.id).label);
};

describe("roster formats", () => {
  it("defaults to Standard, which holds 16 players", () => {
    expect(DEFAULT_FORMAT).toBe("standard");
    expect(rosterLimit("standard")).toBe(16);
  });

  it("Superflex and 2-QB also hold 16 players, with one more starter and one less bench spot", () => {
    expect(rosterLimit("superflex")).toBe(16);
    expect(rosterLimit("two-qb")).toBe(16);
    expect(ROSTER_FORMATS.superflex.starters).toHaveLength(10);
    expect(ROSTER_FORMATS["two-qb"].starters).toHaveLength(10);
  });

  it("every format has one IR spot", () => {
    for (const format of Object.values(ROSTER_FORMATS)) {
      expect(format.ir).toBe(1);
    }
  });
});

describe("isRosterEligible", () => {
  it("allows the fantasy positions, including fullbacks and place kickers", () => {
    for (const position of ["QB", "RB", "FB", "WR", "TE", "PK"]) {
      expect(isRosterEligible(position)).toBe(true);
    }
  });

  it("rejects linemen, individual defenders and other special teams roles", () => {
    for (const position of ["LT", "C", "DE", "LB", "CB", "SS", "P", "LS", "KR"]) {
      expect(isRosterEligible(position)).toBe(false);
    }
  });
});

describe("assignSlots", () => {
  it("fills the starting slots a position is allowed in, in order", () => {
    const players = [player(1, "QB"), player(2, "RB"), player(3, "RB"), player(4, "PK")];

    expect(slotLabels("standard", players)).toEqual(["QB", "RB", "RB", "K"]);
  });

  it("sends a third RB to FLEX, then extra players to the bench", () => {
    const players = [player(1, "RB"), player(2, "RB"), player(3, "RB"), player(4, "RB")];

    expect(slotLabels("standard", players)).toEqual(["RB", "RB", "FLEX", "Bench"]);
    expect(assignSlots("standard", players).get(4)).toBe(BENCH);
  });

  it("puts a second QB on the bench in Standard but starts them in Superflex and 2-QB", () => {
    const players = [player(1, "QB"), player(2, "QB")];

    expect(slotLabels("standard", players)).toEqual(["QB", "Bench"]);
    expect(slotLabels("superflex", players)).toEqual(["QB", "SUPERFLEX"]);
    expect(slotLabels("two-qb", players)).toEqual(["QB", "QB"]);
  });

  it("treats a fullback as a running back", () => {
    expect(slotLabels("standard", [player(1, "FB")])).toEqual(["RB"]);
  });
});

describe("assignSlots with the user's moves", () => {
  it("keeps a move that fits the format, and fills around it", () => {
    const players = [player(1, "RB"), player(2, "RB")];

    // Player 1 was moved to FLEX, so player 2 takes the first RB slot.
    expect(slotLabels("standard", players, { 1: "FLEX" })).toEqual(["FLEX", "RB"]);
  });

  it("keeps players on the bench and IR", () => {
    const players = [player(1, "QB"), player(2, "WR")];
    const slots = assignSlots("standard", players, { 1: "BN", 2: "IR" });

    expect(slots.get(1)).toBe(BENCH);
    expect(slots.get(2)).toBe(IR);
  });

  it("ignores a move into a slot the position isn't allowed in", () => {
    expect(slotLabels("standard", [player(1, "QB")], { 1: "RB1" })).toEqual(["QB"]);
  });

  it("ignores a move into a slot the format doesn't have", () => {
    expect(slotLabels("standard", [player(1, "QB")], { 1: "SFLEX" })).toEqual(["QB"]);
  });

  it("only lets as many players onto IR as the format allows", () => {
    const players = [player(1, "WR"), player(2, "WR")];
    const slots = assignSlots("standard", players, { 1: "IR", 2: "IR" });

    expect(slots.get(1)).toBe(IR);
    expect(slots.get(2)).not.toBe(IR);
  });
});

describe("canMoveTo", () => {
  it("allows a starting slot only for positions listed in it", () => {
    expect(canMoveTo("standard", "RB1", "FB")).toBe(true);
    expect(canMoveTo("standard", "RB1", "QB")).toBe(false);
    expect(canMoveTo("standard", "FLEX", "TE")).toBe(true);
    expect(canMoveTo("superflex", "SFLEX", "QB")).toBe(true);
    expect(canMoveTo("standard", "SFLEX", "QB")).toBe(false);
  });

  it("always allows the bench, and IR until it's full", () => {
    expect(canMoveTo("standard", "BN", "QB")).toBe(true);
    expect(canMoveTo("standard", "IR", "QB")).toBe(true);
    expect(canMoveTo("standard", "IR", "QB", { irFull: true })).toBe(false);
  });
});
