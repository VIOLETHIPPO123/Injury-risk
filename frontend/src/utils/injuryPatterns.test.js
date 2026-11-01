import { describe, expect, it } from "vitest";
import { describeHistory, describeRepeat, repeatedInjuries } from "./injuryPatterns";

const injury = (season, week, bodyPart) => ({
  season, week, bodyPart, type: "Strain", gamesMissed: 1, status: "Recovered",
});

describe("repeatedInjuries", () => {
  it("flags a body part injured twice, with how many seasons it spans", () => {
    const injuries = [injury(2025, 9, "Hamstring"), injury(2024, 1, "Achilles"), injury(2023, 5, "Hamstring")];

    expect(repeatedInjuries(injuries)).toEqual([{ bodyPart: "Hamstring", count: 2, seasons: 2 }]);
  });

  it("does not flag body parts injured only once", () => {
    const injuries = [injury(2025, 9, "Hamstring"), injury(2024, 1, "Ankle"), injury(2023, 5, "Knee")];

    expect(repeatedInjuries(injuries)).toEqual([]);
  });

  it("returns nothing for no injuries", () => {
    expect(repeatedInjuries([])).toEqual([]);
  });

  it("counts two injuries in the same season as one season", () => {
    const injuries = [injury(2025, 12, "Knee"), injury(2025, 3, "Knee")];

    expect(repeatedInjuries(injuries)).toEqual([{ bodyPart: "Knee", count: 2, seasons: 1 }]);
  });

  it("matches body parts regardless of case", () => {
    const injuries = [injury(2025, 9, "Hamstring"), injury(2023, 5, "hamstring")];

    expect(repeatedInjuries(injuries)).toEqual([{ bodyPart: "Hamstring", count: 2, seasons: 2 }]);
  });

  it("lists several repeats, most frequent first", () => {
    const injuries = [
      injury(2026, 2, "Knee"),
      injury(2025, 9, "Hamstring"),
      injury(2025, 4, "Hamstring"),
      injury(2024, 7, "Knee"),
      injury(2023, 5, "Hamstring"),
    ];

    expect(repeatedInjuries(injuries)).toEqual([
      { bodyPart: "Hamstring", count: 3, seasons: 2 },
      { bodyPart: "Knee", count: 2, seasons: 2 },
    ]);
  });
});

describe("describeRepeat", () => {
  it("reads like the ticket's example", () => {
    expect(describeRepeat({ bodyPart: "Hamstring", count: 2, seasons: 2 })).toBe("2 hamstring injuries in 2 seasons.");
  });

  it("says season, not seasons, for one season", () => {
    expect(describeRepeat({ bodyPart: "Knee", count: 2, seasons: 1 })).toBe("2 knee injuries in 1 season.");
  });
});

describe("describeHistory", () => {
  it("summarizes one injury", () => {
    expect(describeHistory([injury(2026, 4, "Ankle")])).toBe("1 injury in 1 season.");
  });

  it("summarizes several injuries with no repeat", () => {
    const injuries = [injury(2025, 3, "Ankle"), injury(2024, 12, "Hip")];

    expect(describeHistory(injuries)).toBe("2 injuries in 2 seasons, none to the same body part twice.");
  });
});
