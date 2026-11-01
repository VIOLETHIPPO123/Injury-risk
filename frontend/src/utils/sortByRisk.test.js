import { describe, expect, it } from "vitest";
import { compareRisk, sortByRisk, teamDefense } from "./sortByRisk";

// 200 snaps over 4 games is a 50-snap average, so ACWR = snapsLastGame / 50.
const player = (name, snapsLastGame, snapsLast4Games = 200, extra = {}) => ({
  name,
  snapsLastGame,
  snapsLast4Games,
  ...extra,
});

const SPIKING = player("Spiking", 80); // 1.60, high, 0.60 from 1.0
const RAMPING = player("Ramping", 20); // 0.40, medium, 0.60 from 1.0
const ELEVATED = player("Elevated", 70); // 1.40, medium, 0.40 from 1.0
const NORMAL = player("Normal", 50); // 1.00, low
const NO_DATA = player("No Data", 30, 0); // no 4-game history, so no ACWR

const names = (entries) => entries.map((e) => e.name);

describe("compareRisk", () => {
  it("is positive when the first ACWR is riskier, negative when the second is", () => {
    expect(compareRisk(1.6, 1.4)).toBeGreaterThan(0);
    expect(compareRisk(1.4, 1.6)).toBeLessThan(0);
  });

  it("is 0 when the risk is level", () => {
    expect(compareRisk(1.1, 1.1)).toBe(0);
  });
});

describe("sortByRisk", () => {
  it("ranks a Spiking player above any Slightly elevated player", () => {
    // 1.31 and 1.50 are the lowest and highest ACWRs that count as Slightly elevated.
    const lowestElevated = player("Aaron Low", 65.5); // 1.31
    const highestElevated = player("Abe High", 75); // 1.50
    const justSpiking = player("Zack Spike", 75.5); // 1.51

    const sorted = sortByRisk([lowestElevated, highestElevated, justSpiking]);

    expect(names(sorted)).toEqual(["Zack Spike", "Abe High", "Aaron Low"]);
  });

  it("ranks a higher tier first even when a lower tier is further from 1.0", () => {
    const farRamping = player("Far Ramping", 10); // 0.20, medium, 0.80 from 1.0

    expect(names(sortByRisk([farRamping, SPIKING]))).toEqual(["Spiking", "Far Ramping"]);
  });

  it("within a tier, ranks the ACWR further from 1.0 higher, on either side of 1.0", () => {
    // Both medium: 0.40 is 0.60 from 1.0, and 1.40 is only 0.40 away.
    expect(names(sortByRisk([ELEVATED, RAMPING]))).toEqual(["Ramping", "Elevated"]);
  });

  it("puts the whole list in order: tier, then distance from 1.0, then no data", () => {
    const sorted = sortByRisk([NORMAL, ELEVATED, NO_DATA, SPIKING, RAMPING]);

    expect(names(sorted)).toEqual(["Spiking", "Ramping", "Elevated", "Normal", "No Data"]);
  });

  it("reverses the order for asc", () => {
    const sorted = sortByRisk([SPIKING, NORMAL, RAMPING, ELEVATED], "asc");

    expect(names(sorted)).toEqual(["Normal", "Elevated", "Ramping", "Spiking"]);
  });

  it("orders players with identical risk alphabetically, in both directions", () => {
    const brown = player("Brown", 50);
    const adams = player("Adams", 50);

    expect(names(sortByRisk([brown, adams], "desc"))).toEqual(["Adams", "Brown"]);
    expect(names(sortByRisk([brown, adams], "asc"))).toEqual(["Adams", "Brown"]);
  });

  it("puts players with no ACWR last in both directions, alphabetically", () => {
    const zed = player("Zed", 30, 0);
    const players = [zed, NORMAL, NO_DATA, SPIKING];

    expect(names(sortByRisk(players, "desc"))).toEqual(["Spiking", "Normal", "No Data", "Zed"]);
    expect(names(sortByRisk(players, "asc"))).toEqual(["Normal", "Spiking", "No Data", "Zed"]);
  });

  it("does not change the list it was given", () => {
    const players = [NORMAL, SPIKING];

    sortByRisk(players);

    expect(names(players)).toEqual(["Normal", "Spiking"]);
  });
});

describe("teamDefense", () => {
  const onTeam = (team, position, snapsLastGame, snapsLast4Games = 200) =>
    player(`${team} ${position}`, snapsLastGame, snapsLast4Games, { team, position });

  it("averages the ACWR of the team's defensive and special teams players", () => {
    const players = [
      onTeam("ARI", "LB", 80), // 1.60, defense
      onTeam("ARI", "PK", 40), // 0.80, special teams
      onTeam("ARI", "QB", 10), // offense: not part of D/ST
      onTeam("ATL", "LB", 10), // another team
    ];

    const dst = teamDefense(players, "ARI");

    expect(dst).toMatchObject({ name: "ARI D/ST", team: "ARI", position: "D/ST" });
    expect(dst.acwr).toBeCloseTo(1.2);
  });

  it("leaves players with no ACWR out of the average", () => {
    const players = [onTeam("ARI", "CB", 60), onTeam("ARI", "LS", 30, 0)];

    expect(teamDefense(players, "ARI").acwr).toBeCloseTo(1.2);
  });

  it("has no ACWR when none of its players has one", () => {
    expect(teamDefense([onTeam("ARI", "SS", 30, 0)], "ARI").acwr).toBeNull();
  });

  it("ranks D/ST units with the same rule as players", () => {
    const players = [
      onTeam("ARI", "DE", 80), // ARI D/ST: 1.60, Spiking
      onTeam("ATL", "DE", 55), // ATL D/ST: 1.10, Normal
      onTeam("BAL", "DE", 30, 0), // BAL D/ST: no data
    ];
    const units = ["BAL", "ATL", "ARI"].map((team) => teamDefense(players, team));

    expect(names(sortByRisk(units))).toEqual(["ARI D/ST", "ATL D/ST", "BAL D/ST"]);
  });
});
