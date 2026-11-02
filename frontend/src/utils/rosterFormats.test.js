import { describe, expect, it } from "vitest";
import {
  BENCH,
  canMoveTo,
  DEFAULT_FORMAT,
  IR,
  isRosterEligible,
  placeEntries,
  rosterLimit,
} from "./rosterFormats";
import { lineupFormat } from "./rosterFormats.fixture";

const ESPN = lineupFormat("espn-standard");
const YAHOO = lineupFormat("yahoo-standard");
const SLEEPER = lineupFormat("sleeper-default");

// Roster entries as the backend sends them, in the order they were added.
const entry = (playerId, slot) => ({ id: playerId, playerId, slot, note: "" });
const spotIds = (format, entries) => {
  const spots = placeEntries(format, entries);
  return entries.map((e) => spots.get(e.playerId).id);
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

describe("placeEntries", () => {
  it("puts each player in the spot for their backend slot", () => {
    const entries = [entry(1, "QB"), entry(2, "K"), entry(3, "DEF"), entry(4, "FLEX")];

    expect(spotIds(ESPN, entries)).toEqual(["QB", "K", "DEF", "FLEX"]);
  });

  it("fills a slot's spots in the order players were added", () => {
    expect(spotIds(ESPN, [entry(1, "RB"), entry(2, "RB")])).toEqual(["RB1", "RB2"]);
    expect(spotIds(SLEEPER, [entry(1, "FLEX"), entry(2, "FLEX")])).toEqual(["FLEX1", "FLEX2"]);
  });

  it("puts BENCH and IR players on the shared bench and IR spots", () => {
    const spots = placeEntries(ESPN, [entry(1, "BENCH"), entry(2, "BENCH"), entry(3, "IR")]);

    expect(spots.get(1)).toBe(BENCH);
    expect(spots.get(2)).toBe(BENCH);
    expect(spots.get(3)).toBe(IR);
  });

  it("benches a player whose slot the format doesn't have, or has no room left in", () => {
    // ESPN Standard has one FLEX spot and no SUPERFLEX.
    expect(spotIds(ESPN, [entry(1, "FLEX"), entry(2, "FLEX")])).toEqual(["FLEX", "BN"]);
    expect(spotIds(ESPN, [entry(1, "SUPERFLEX")])).toEqual(["BN"]);
    expect(spotIds(ESPN, [entry(1, undefined)])).toEqual(["BN"]);
  });

  it("gives every spot the backend's slot name to move into", () => {
    expect(ESPN.starters.find((s) => s.id === "RB2").slot).toBe("RB");
    expect(BENCH.slot).toBe("BENCH");
    expect(IR.slot).toBe("IR");
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
