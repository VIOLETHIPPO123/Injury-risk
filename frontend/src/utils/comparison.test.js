import { describe, expect, it } from "vitest";
import { compareVerdict, comparisonRows } from "./comparison";

// 200 snaps over 4 games is a 50-snap average, so ACWR = snapsLastGame / 50.
function player(name, snapsLastGame, snapsLast4Games = 200) {
  return { id: name, name, position: "LG", snapsLastGame, snapsLast4Games };
}

const LOW = player("Low Larry", 55); // 1.10, Normal workload
const LOW_FURTHER = player("Low Lou", 60); // 1.20, Normal workload
const MEDIUM_HIGH_SIDE = player("Medium Mo", 70); // 1.40, Slightly elevated
const MEDIUM_LOW_SIDE = player("Ramping Ray", 38); // 0.76, Ramping up
const HIGH = player("High Hal", 80); // 1.60, Spiking
const NO_DATA = player("New Ned", 30, 0); // no 4-game history

describe("compareVerdict", () => {
  describe("different tiers", () => {
    it("names the player in the lower tier as safer", () => {
      expect(compareVerdict(HIGH, LOW)).toBe("Safer start: Low Larry.");
    });

    it("gives the same answer whichever column each player is in", () => {
      expect(compareVerdict(LOW, HIGH)).toBe("Safer start: Low Larry.");
    });
  });

  describe("same tier", () => {
    it("names the player whose ACWR is closer to 1.0 as safer", () => {
      expect(compareVerdict(LOW_FURTHER, LOW)).toBe("Safer start: Low Larry.");
    });

    it("measures distance from 1.0 in both directions", () => {
      // Both are medium: 1.40 is 0.40 from 1.0, 0.76 is only 0.24 away.
      expect(compareVerdict(MEDIUM_HIGH_SIDE, MEDIUM_LOW_SIDE)).toBe(
        "Safer start: Ramping Ray.",
      );
    });

    it("calls an exact tie too close to call", () => {
      expect(compareVerdict(LOW, player("Twin Tim", 55))).toBe(
        "Too close to call.",
      );
    });
  });

  describe("missing data", () => {
    it("can't call it when the first player has no ACWR", () => {
      expect(compareVerdict(NO_DATA, LOW)).toBe("Not enough data to call it.");
    });

    it("can't call it when the second player has no ACWR", () => {
      expect(compareVerdict(HIGH, NO_DATA)).toBe("Not enough data to call it.");
    });
  });
});

describe("comparisonRows", () => {
  it("shows all four stats for both players", () => {
    const rows = comparisonRows(HIGH, LOW);

    expect(rows.map((row) => row.label)).toEqual([
      "ACWR",
      "Risk tier",
      "Last game snaps",
      "4-game average",
    ]);
    expect(rows.map((row) => row.values)).toEqual([
      ["1.60", "1.10"],
      ["Spiking — high risk", "Normal workload"],
      ["80", "55"],
      ["50.00", "50.00"],
    ]);
  });

  it("marks the riskier value in each row, and nothing when the values are level", () => {
    const rows = comparisonRows(LOW, HIGH);

    expect(rows.map((row) => row.riskier)).toEqual([1, 1, 1, null]);
  });

  it("doesn't mark ACWR or risk tier when one player has no data", () => {
    const [acwr, tier] = comparisonRows(NO_DATA, LOW);

    expect(acwr.values).toEqual(["—", "1.10"]);
    expect(acwr.riskier).toBeNull();
    expect(tier.riskier).toBeNull();
  });
});
