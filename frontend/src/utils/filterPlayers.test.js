import { expect, test } from "vitest";
import { EMPTY_SEARCH, filterPlayers } from "./filterPlayers";

const LAMB = { id: 1, name: "CeeDee Lamb", team: "DAL", position: "WR" };
const PRESCOTT = { id: 2, name: "Dak Prescott", team: "DAL", position: "QB" };
const BROWN = { id: 3, name: "A.J. Brown", team: "PHI", position: "WR" };
const BARKLEY = { id: 4, name: "Saquon Barkley", team: "PHI", position: "RB" };
const NABERS = { id: 5, name: "Malik Nabers", team: "NYG", position: "WR" };
const PLAYERS = [LAMB, PRESCOTT, BROWN, BARKLEY, NABERS];

const search = (changes) => ({ ...EMPTY_SEARCH, ...changes });

test("an empty search returns every player", () => {
  expect(filterPlayers(PLAYERS, EMPTY_SEARCH)).toEqual(PLAYERS);
});

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
