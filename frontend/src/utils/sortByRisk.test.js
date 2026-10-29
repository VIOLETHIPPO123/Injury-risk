import { describe, expect, it } from "vitest";
import { sortByRisk } from "./sortByRisk";

const player = (name, snapsLastGame, snapsLast4Games) => ({ name, snapsLastGame, snapsLast4Games });

// ACWR = last game ÷ (last four ÷ 4).
const SPIKING = player("Spiking", 80, 200); // 1.6, high
const RAMPING = player("Ramping", 20, 200); // 0.4, medium, 0.6 from 1.0
const ELEVATED = player("Elevated", 70, 200); // 1.4, medium, 0.4 from 1.0
const NORMAL = player("Normal", 50, 200); // 1.0, low
const NO_DATA = player("No Data", 0, 0); // no ACWR

const names = (players) => players.map((p) => p.name);

describe("sortByRisk", () => {
  it("puts the highest tier first, then the ACWR furthest from 1.0", () => {
    const sorted = sortByRisk([NORMAL, ELEVATED, NO_DATA, SPIKING, RAMPING]);

    expect(names(sorted)).toEqual(["Spiking", "Ramping", "Elevated", "Normal", "No Data"]);
  });

  it("reverses the order for asc, but keeps players with no data last", () => {
    const sorted = sortByRisk([NO_DATA, SPIKING, NORMAL, RAMPING, ELEVATED], "asc");

    expect(names(sorted)).toEqual(["Normal", "Elevated", "Ramping", "Spiking", "No Data"]);
  });

  it("breaks ties by name in both directions", () => {
    const brown = player("Brown", 50, 200);
    const adams = player("Adams", 50, 200);

    expect(names(sortByRisk([brown, adams], "desc"))).toEqual(["Adams", "Brown"]);
    expect(names(sortByRisk([brown, adams], "asc"))).toEqual(["Adams", "Brown"]);
  });

  it("orders several players with no data by name", () => {
    const zed = player("Zed", 0, 0);

    expect(names(sortByRisk([zed, NO_DATA, NORMAL]))).toEqual(["Normal", "No Data", "Zed"]);
  });

  it("does not change the list it was given", () => {
    const players = [NORMAL, SPIKING];

    sortByRisk(players);

    expect(names(players)).toEqual(["Normal", "Spiking"]);
  });
});
