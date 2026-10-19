import { describe, expect, test } from "vitest";
import { EMPTY_SEARCH, filterPlayers, hasActiveSearch } from "./filterPlayers";

const LAMB = { id: 1, name: "CeeDee Lamb", team: "DAL", position: "WR" };
const PRESCOTT = { id: 2, name: "Dak Prescott", team: "DAL", position: "QB" };
const BROWN = { id: 3, name: "A.J. Brown", team: "PHI", position: "WR" };
const BARKLEY = { id: 4, name: "Saquon Barkley", team: "PHI", position: "RB" };
const NABERS = { id: 5, name: "Malik Nabers", team: "NYG", position: "WR" };
const PLAYERS = [LAMB, PRESCOTT, BROWN, BARKLEY, NABERS];

const ALLEN = { id: 6, name: "Josh Allen", team: "BUF", position: "QB" };
const JACOBS = { id: 7, name: "Josh Jacobs", team: "GB", position: "RB" };
const JONES = { id: 8, name: "Daniel Jones", team: "IND", position: "QB" };
const CHASE = { id: 9, name: "Ja'Marr Chase", team: "CIN", position: "WR" };
const ST_BROWN = { id: 10, name: "Amon-Ra St. Brown", team: "DET", position: "WR" };
const HARRISON = { id: 11, name: "Marvin Harrison Jr.", team: "ARI", position: "WR" };
const NAMES = [ALLEN, JACOBS, JONES, CHASE, ST_BROWN, HARRISON];

const search = (changes) => ({ ...EMPTY_SEARCH, ...changes });
const byName = (players, query) => filterPlayers(players, search({ query }));

// ---------- Blank default ----------

describe("with nothing to search on", () => {
  test("an empty search returns no players", () => {
    expect(filterPlayers(PLAYERS, EMPTY_SEARCH)).toEqual([]);
    expect(hasActiveSearch(EMPTY_SEARCH)).toBe(false);
  });

  test("a whitespace-only query returns no players", () => {
    expect(byName(PLAYERS, "   ")).toEqual([]);
  });

  test("a 1-character query returns no players", () => {
    expect(byName(NAMES, "j")).toEqual([]);
    expect(hasActiveSearch(search({ query: "j" }))).toBe(false);
  });

  test("punctuation doesn't count toward the 2 characters", () => {
    expect(byName(NAMES, "j'")).toEqual([]);
  });

  test("a 2-character query returns matching players", () => {
    expect(byName(NAMES, "ja")).toEqual([JACOBS, CHASE]);
    expect(hasActiveSearch(search({ query: "ja" }))).toBe(true);
  });

  test("a 1-character query is ignored when chips are selected", () => {
    expect(filterPlayers(PLAYERS, search({ query: "x", teams: ["PHI"] }))).toEqual([BROWN, BARKLEY]);
  });
});

// ---------- Word-start matching ----------

describe("word-start matching", () => {
  test('"jo al" returns Josh Allen', () => {
    expect(byName(NAMES, "jo al")).toEqual([ALLEN]);
  });

  test('"jamarr" returns Ja\'Marr Chase, ignoring case and punctuation', () => {
    expect(byName(NAMES, "jamarr")).toEqual([CHASE]);
    expect(byName(NAMES, "JA'MARR")).toEqual([CHASE]);
  });

  test("periods in initials are ignored", () => {
    expect(byName(PLAYERS, "aj")).toEqual([BROWN]);
  });

  test("a query can run across hyphens and spaces in a name", () => {
    expect(byName(NAMES, "amonra")).toEqual([ST_BROWN]);
    expect(byName(NAMES, "st brown")).toEqual([ST_BROWN]);
    expect(byName(NAMES, "stbrown")).toEqual([ST_BROWN]);
  });

  test("text in the middle of a word doesn't match", () => {
    expect(byName(NAMES, "llen")).toEqual([]);
    expect(byName(PLAYERS, "amb")).toEqual([]);
  });

  test("every word of the query must match", () => {
    expect(byName(NAMES, "josh zz")).toEqual([]);
  });

  test("a query that matches nobody returns an empty list", () => {
    expect(byName(PLAYERS, "zz")).toEqual([]);
  });
});

// ---------- Ranking ----------

describe("ranking", () => {
  test("a last-name match ranks above a first-name match", () => {
    const DANIELS = { id: 12, name: "Jayden Daniels", team: "WAS", position: "QB" };
    // Daniel Jones' first name and Jayden Daniels' last name both start with "dan".
    expect(byName([JONES, DANIELS], "dan")).toEqual([DANIELS, JONES]);
  });

  test("last-name matches come first, then first-name matches, each alphabetical", () => {
    // Jones matches by last name; both Joshes by first name, sorted Allen then Jacobs.
    expect(byName(NAMES, "jo")).toEqual([JONES, ALLEN, JACOBS]);
  });

  test("ties within a tier sort alphabetically by last name", () => {
    expect(byName(NAMES, "josh")).toEqual([ALLEN, JACOBS]);
  });

  test("a suffix like Jr. isn't treated as the last name", () => {
    const JR = { id: 13, name: "Jr Smith", team: "NE", position: "WR" };
    // Harrison still matches on "Jr.", but below Jr Smith's first-name match.
    expect(byName([HARRISON, JR], "jr")).toEqual([JR, HARRISON]);
  });
});

// ---------- Chips ----------

describe("position and team chips", () => {
  test("filters alone, with no query, return results", () => {
    expect(filterPlayers(PLAYERS, search({ positions: ["WR"] }))).toEqual([LAMB, BROWN, NABERS]);
  });

  test("within one category a player can match any chip (QB or RB)", () => {
    expect(filterPlayers(PLAYERS, search({ positions: ["QB", "RB"] }))).toEqual([PRESCOTT, BARKLEY]);
  });

  test("across categories a player must match all of them (WR and DAL)", () => {
    expect(filterPlayers(PLAYERS, search({ positions: ["WR"], teams: ["DAL"] }))).toEqual([LAMB]);
  });

  test("several chips in both categories combine as (any position) and (any team)", () => {
    const result = filterPlayers(PLAYERS, search({ positions: ["WR", "RB"], teams: ["PHI", "NYG"] }));
    expect(result).toEqual([BROWN, BARKLEY, NABERS]);
  });

  test("the name query applies on top of the chips", () => {
    expect(filterPlayers(PLAYERS, search({ query: "brown", positions: ["WR"] }))).toEqual([BROWN]);
  });

  test("no player matching every category returns an empty list", () => {
    expect(filterPlayers(PLAYERS, search({ positions: ["RB"], teams: ["DAL"] }))).toEqual([]);
  });
});
