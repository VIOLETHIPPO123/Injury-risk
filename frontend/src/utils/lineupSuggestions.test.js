import { describe, expect, it } from "vitest";
import { benchSuggestions } from "./lineupSuggestions";
import { BENCH } from "./rosterFormats";
import { lineupFormat } from "./rosterFormats.fixture";

const slot = (id) => lineupFormat("espn-standard").starters.find((s) => s.id === id);
// ACWR = last game ÷ (last four ÷ 4).
const player = (id, name, position, snapsLastGame, inSlot) => ({
  id, name, position, snapsLastGame, snapsLast4Games: 200, slot: inSlot,
});

const SPIKING_RB = player(1, "Spiking RB", "RB", 80, slot("RB1")); // 1.6, high
const NORMAL_RB = player(2, "Normal RB", "RB", 50, BENCH); // 1.0, low
const ELEVATED_RB = player(3, "Elevated RB", "RB", 70, BENCH); // 1.4, medium

describe("benchSuggestions", () => {
  it("suggests the bench player when the swap lowers the risk", () => {
    expect(benchSuggestions([SPIKING_RB, NORMAL_RB]).map((s) => s.text)).toEqual([
      "Consider starting Normal RB over Spiking RB.",
    ]);
  });

  it("picks the lowest-risk bench player when several would help", () => {
    const [suggestion] = benchSuggestions([SPIKING_RB, ELEVATED_RB, NORMAL_RB]);

    expect(suggestion.benchPlayer).toBe(NORMAL_RB);
  });

  it("ignores starters who aren't high risk", () => {
    const elevatedStarter = { ...ELEVATED_RB, slot: slot("RB1") };

    expect(benchSuggestions([elevatedStarter, NORMAL_RB])).toEqual([]);
  });

  it("ignores bench players who aren't allowed in the starter's slot", () => {
    const benchWr = player(4, "Bench WR", "WR", 50, BENCH);

    expect(benchSuggestions([SPIKING_RB, benchWr])).toEqual([]);
  });

  it("ignores bench players who are as risky or riskier", () => {
    const worseRb = player(5, "Worse RB", "RB", 90, BENCH); // 1.8, high and further from 1.0

    expect(benchSuggestions([SPIKING_RB, worseRb])).toEqual([]);
  });

  it("ignores bench players with no data", () => {
    const noData = { ...player(6, "No Data RB", "RB", 0, BENCH), snapsLast4Games: 0 };

    expect(benchSuggestions([SPIKING_RB, noData])).toEqual([]);
  });

  it("lets a FLEX starter be replaced by any FLEX-eligible bench player", () => {
    const flexStarter = player(7, "Spiking WR", "WR", 80, slot("FLEX"));

    expect(benchSuggestions([flexStarter, NORMAL_RB]).map((s) => s.benchPlayer)).toEqual([NORMAL_RB]);
  });

  it("suggests each bench player only once", () => {
    const otherSpiking = player(8, "Other Spiking RB", "RB", 85, slot("RB2"));

    expect(benchSuggestions([SPIKING_RB, otherSpiking, NORMAL_RB])).toHaveLength(1);
  });
});
