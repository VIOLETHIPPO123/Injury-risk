import { describe, expect, it } from "vitest";
import { calculateAcwr, weeklyAcwr } from "./acwr";

// Builds games the way GET /players/{id}/workload sends them, from snaps per week starting
// at week 1. null is a bye week, which has no entry.
const history = (snapsPerWeek) =>
  snapsPerWeek
    .map((snaps, i) => ({ playerId: 1, week: i + 1, snaps }))
    .filter((game) => game.snaps !== null);

const acwrs = (weeks) => weeks.map((w) => w.acwr);

describe("weeklyAcwr", () => {
  it("scores a full season: no value for the first three games, then one per game", () => {
    // 16 steady games of 50 snaps, then a jump to 80 in week 17.
    const weeks = weeklyAcwr(history([...Array(16).fill(50), 80]));

    expect(weeks).toHaveLength(17);
    expect(acwrs(weeks).slice(0, 3)).toEqual([null, null, null]);
    expect(acwrs(weeks).slice(3, 16)).toEqual(Array(13).fill(1));
    // Last four games: 50 + 50 + 50 + 80 = 230, an average of 57.5.
    expect(weeks[16].acwr).toBeCloseTo(80 / 57.5);
  });

  it("leaves a gap on a bye week and doesn't count it as 0", () => {
    const weeks = weeklyAcwr(history([50, 50, 50, 50, null, 80]));

    expect(weeks[4]).toEqual({ week: 5, snaps: null, acwr: null });
    // The four games before week 6 are weeks 2, 3, 4 and 6 (230 snaps), not weeks 3 to 6.
    // Counting the bye as 0 would give 80 / 45 = 1.78 instead.
    expect(weeks[5].acwr).toBeCloseTo(80 / 57.5);
  });

  it("skips a bye when counting a player's first four games", () => {
    const weeks = weeklyAcwr(history([50, 50, null, 50, 50]));

    // Week 4 is only the third game; week 5 is the fourth.
    expect(acwrs(weeks)).toEqual([null, null, null, null, 1]);
  });

  it("has no value for a player with fewer than 4 games", () => {
    const weeks = weeklyAcwr(history([50, 60, 70]));

    expect(acwrs(weeks)).toEqual([null, null, null]);
  });

  it("counts a game with 0 snaps as a game, unlike a bye", () => {
    const weeks = weeklyAcwr(history([50, 50, 50, 0, 50]));

    expect(weeks[3].acwr).toBe(0);
    // Last four games: 50 + 50 + 0 + 50 = 150, an average of 37.5.
    expect(weeks[4].acwr).toBeCloseTo(50 / 37.5);
  });

  it("ends on the same ACWR the player page shows", () => {
    // Jacoby Brissett's games from GET /players/1/workload (bye in week 5). The player
    // page scores him from his stored snaps: 68 last game, 253 over the last four.
    const weeks = weeklyAcwr(history([65, 57, 61, 62, null, 62, 61, 68]));

    expect(weeks.at(-1).acwr).toBe(calculateAcwr(68, 253));
  });

  it("fills in a bye in week 1 as a gap too", () => {
    expect(weeklyAcwr(history([null, 50]))).toEqual([
      { week: 1, snaps: null, acwr: null },
      { week: 2, snaps: 50, acwr: null },
    ]);
  });

  it("returns nothing for an empty history", () => {
    expect(weeklyAcwr([])).toEqual([]);
  });
});
