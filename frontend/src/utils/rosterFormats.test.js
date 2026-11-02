import { describe, expect, it } from "vitest";
import {
  assignSlots,
  BENCH,
  canMoveTo,
  DEFAULT_FORMAT,
  IR,
  isRosterEligible,
  rosterLimit,
} from "./rosterFormats";
import { lineupFormat } from "./rosterFormats.fixture";

const ESPN = lineupFormat("espn-standard");
const YAHOO = lineupFormat("yahoo-standard");
const SLEEPER = lineupFormat("sleeper-default");

const player = (id, position) => ({ id, position });
const slotLabels = (format, players, assignments = {}) => {
  const slots = assignSlots(format, players, assignments);
  return players.map((p) => slots.get(p.id).label);
};

describe("toLineupFormat", () => {
  it("defaults to ESPN Standard, like the backend", () => {
    expect(DEFAULT_FORMAT).toBe("espn-standard");
  });

  it("ESPN Standard: 9 starters, 7 bench, 1 IR, 16 players", () => {
    expect(ESPN.label).toBe("ESPN Standard");
    expect(ESPN.starters.map((s) => s.id)).toEqual([
      "QB", "RB1", "RB2", "WR1", "WR2", "TE", "K", "DEF", "FLEX",
    ]);
    expect(ESPN.bench).toBe(7);
    expect(ESPN.ir).toBe(1);
    expect(rosterLimit(ESPN)).toBe(16);
  });

  it("Yahoo Standard: 9 starters, 6 bench, 2 IR, 15 players", () => {
    expect(YAHOO.starters).toHaveLength(9);
    expect(YAHOO.bench).toBe(6);
    expect(YAHOO.ir).toBe(2);
    expect(rosterLimit(YAHOO)).toBe(15);
  });

  it("Sleeper Default: two FLEX spots, 5 bench, no IR, 15 players", () => {
    expect(SLEEPER.starters.map((s) => s.id)).toEqual([
      "QB", "RB1", "RB2", "WR1", "WR2", "TE", "K", "DEF", "FLEX1", "FLEX2",
    ]);
    expect(SLEEPER.bench).toBe(5);
    expect(SLEEPER.ir).toBe(0);
    expect(rosterLimit(SLEEPER)).toBe(15);
  });

  it("every format's starters plus bench is its roster limit", () => {
    for (const format of [ESPN, YAHOO, SLEEPER]) {
      expect(format.starters.length + format.bench).toBe(rosterLimit(format));
    }
  });

  it("lets FB into RB slots, PK into K and D/ST into DEF", () => {
    const allowed = (id) => ESPN.starters.find((s) => s.id === id).allowed;
    expect(allowed("RB1")).toEqual(["RB", "FB"]);
    expect(allowed("K")).toEqual(["PK"]);
    expect(allowed("DEF")).toEqual(["D/ST"]);
    expect(allowed("FLEX")).toEqual(["RB", "FB", "WR", "TE"]);
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

    expect(slotLabels(ESPN, players)).toEqual(["QB", "RB", "RB", "K"]);
  });

  it("sends a third RB to FLEX, then extra players to the bench", () => {
    const players = [player(1, "RB"), player(2, "RB"), player(3, "RB"), player(4, "RB")];

    expect(slotLabels(ESPN, players)).toEqual(["RB", "RB", "FLEX", "Bench"]);
    expect(assignSlots(ESPN, players).get(4)).toBe(BENCH);
  });

  it("starts a fourth RB in Sleeper's second FLEX", () => {
    const players = [player(1, "RB"), player(2, "RB"), player(3, "RB"), player(4, "RB")];

    expect(slotLabels(SLEEPER, players)).toEqual(["RB", "RB", "FLEX", "FLEX"]);
  });

  it("puts a second QB on the bench", () => {
    expect(slotLabels(ESPN, [player(1, "QB"), player(2, "QB")])).toEqual(["QB", "Bench"]);
  });

  it("treats a fullback as a running back", () => {
    expect(slotLabels(ESPN, [player(1, "FB")])).toEqual(["RB"]);
  });
});

describe("assignSlots with the user's moves", () => {
  it("keeps a move that fits the format, and fills around it", () => {
    const players = [player(1, "RB"), player(2, "RB")];

    // Player 1 was moved to FLEX, so player 2 takes the first RB slot.
    expect(slotLabels(ESPN, players, { 1: "FLEX" })).toEqual(["FLEX", "RB"]);
  });

  it("keeps players on the bench and IR", () => {
    const players = [player(1, "QB"), player(2, "WR")];
    const slots = assignSlots(ESPN, players, { 1: "BN", 2: "IR" });

    expect(slots.get(1)).toBe(BENCH);
    expect(slots.get(2)).toBe(IR);
  });

  it("ignores a move into a slot the position isn't allowed in", () => {
    expect(slotLabels(ESPN, [player(1, "QB")], { 1: "RB1" })).toEqual(["QB"]);
  });

  it("ignores a move into a slot the format doesn't have", () => {
    expect(slotLabels(ESPN, [player(1, "RB")], { 1: "FLEX2" })).toEqual(["RB"]);
  });

  it("only lets as many players onto IR as the format allows", () => {
    const players = [player(1, "WR"), player(2, "WR"), player(3, "WR")];
    const espn = assignSlots(ESPN, players, { 1: "IR", 2: "IR" });
    const yahoo = assignSlots(YAHOO, players, { 1: "IR", 2: "IR", 3: "IR" });
    const sleeper = assignSlots(SLEEPER, players, { 1: "IR" });

    expect(espn.get(1)).toBe(IR);
    expect(espn.get(2)).not.toBe(IR);
    expect(yahoo.get(1)).toBe(IR);
    expect(yahoo.get(2)).toBe(IR);
    expect(yahoo.get(3)).not.toBe(IR);
    expect(sleeper.get(1)).not.toBe(IR);
  });
});

describe("canMoveTo", () => {
  it("allows a starting slot only for positions listed in it", () => {
    expect(canMoveTo(ESPN, "RB1", "FB")).toBe(true);
    expect(canMoveTo(ESPN, "RB1", "QB")).toBe(false);
    expect(canMoveTo(ESPN, "FLEX", "TE")).toBe(true);
    expect(canMoveTo(ESPN, "FLEX", "QB")).toBe(false);
    expect(canMoveTo(SLEEPER, "FLEX2", "WR")).toBe(true);
    expect(canMoveTo(ESPN, "FLEX2", "WR")).toBe(false);
  });

  it("always allows the bench, and IR until it's full", () => {
    expect(canMoveTo(ESPN, "BN", "QB")).toBe(true);
    expect(canMoveTo(ESPN, "IR", "QB")).toBe(true);
    expect(canMoveTo(ESPN, "IR", "QB", { irFull: true })).toBe(false);
  });
});
