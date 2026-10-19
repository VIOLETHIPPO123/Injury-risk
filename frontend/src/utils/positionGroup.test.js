import { describe, expect, it } from "vitest";
import {
  compareCandidates,
  positionGroup,
  positionsInGroup,
} from "./positionGroup";

describe("positionGroup", () => {
  it.each([
    ["LT", "OL"],
    ["LG", "OL"],
    ["C", "OL"],
    ["RG", "OL"],
    ["RT", "OL"],
    ["DE", "DL"],
    ["DT", "DL"],
    ["LB", "LB"],
    ["CB", "DB"],
    ["S", "DB"],
    ["FS", "DB"],
    ["SS", "DB"],
    ["RB", "RB"],
    ["FB", "RB"],
    ["QB", "QB"],
    ["WR", "WR"],
    ["TE", "TE"],
    ["K", "K"],
    ["PK", "K"],
    ["P", "P"],
    ["LS", "LS"],
    ["H", "H"],
    ["KR", "KR"],
    ["PR", "PR"],
  ])("puts %s in the %s group", (position, group) => {
    expect(positionGroup(position)).toBe(group);
  });
});

describe("positionsInGroup", () => {
  it.each([
    ["OL", ["LT", "LG", "C", "RG", "RT"]],
    ["DL", ["DE", "DT"]],
    ["LB", ["LB"]],
    ["DB", ["CB", "S", "FS", "SS"]],
    ["RB", ["RB", "FB"]],
    ["K", ["K", "PK"]],
    ["QB", ["QB"]],
    ["WR", ["WR"]],
    ["TE", ["TE"]],
    ["P", ["P"]],
    ["LS", ["LS"]],
  ])("lists the %s group as %j", (group, positions) => {
    expect(positionsInGroup(group)).toEqual(positions);
  });

  it("round-trips: every position in a group maps back to that group", () => {
    for (const group of ["OL", "DL", "DB", "RB", "K"]) {
      for (const position of positionsInGroup(group)) {
        expect(positionGroup(position)).toBe(group);
      }
    }
  });
});

function player(id, position) {
  return { id, name: `Player ${id}`, team: "ARI", position };
}

describe("compareCandidates", () => {
  it("lists a watchlisted RT for an LG", () => {
    const lg = player(1, "LG");
    const rt = player(2, "RT");

    expect(compareCandidates(lg, [rt])).toEqual([rt]);
  });

  it("leaves out watchlisted players from other groups", () => {
    const lg = player(1, "LG");
    const qb = player(2, "QB");
    const de = player(3, "DE");

    expect(compareCandidates(lg, [qb, de])).toEqual([]);
  });

  it("never lists the current player", () => {
    const lg = player(1, "LG");
    const c = player(2, "C");

    expect(compareCandidates(lg, [lg, c])).toEqual([c]);
  });

  it("is empty when the current player is the only one in their group", () => {
    const lg = player(1, "LG");

    expect(compareCandidates(lg, [lg])).toEqual([]);
  });
});
