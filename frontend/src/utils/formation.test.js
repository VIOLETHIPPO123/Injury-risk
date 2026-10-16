import { expect, test } from "vitest";
import { Slot, createFormation } from "./formation";

// Made-up positions on purpose: createFormation must work for any definition,
// not just football ones.
const player = (id, position) => ({ id, position });
const A1 = player(1, "A");
const A2 = player(2, "A");
const A3 = player(3, "A");
const B1 = player(4, "B");
const B2 = player(5, "B");

const byPosition = { A: [A1, A2, A3], B: [B1, B2] };

// ---------- Slot.pick ----------

test("Slot.pick returns the first player at a position by default", () => {
  expect(Slot.pick("A").resolve(byPosition)).toEqual([A1]);
});

test("Slot.pick returns the nth player at a position", () => {
  expect(Slot.pick("A", 2).resolve(byPosition)).toEqual([A3]);
});

test("Slot.pick yields no player when the position or index is missing", () => {
  expect(Slot.pick("Z").resolve(byPosition).filter(Boolean)).toEqual([]);
  expect(Slot.pick("B", 5).resolve(byPosition).filter(Boolean)).toEqual([]);
});

// ---------- Slot.from ----------

test("Slot.from returns every player at a position by default", () => {
  expect(Slot.from("A").resolve(byPosition)).toEqual([A1, A2, A3]);
});

test("Slot.from returns the players from a start index on", () => {
  expect(Slot.from("A", 1).resolve(byPosition)).toEqual([A2, A3]);
});

test("Slot.from returns an empty list past the end or for a missing position", () => {
  expect(Slot.from("A", 3).resolve(byPosition)).toEqual([]);
  expect(Slot.from("Z").resolve(byPosition)).toEqual([]);
});

// ---------- Slot.centered ----------

test("Slot.centered splits an even number of edge players around the middle", () => {
  const slot = Slot.centered(Slot.from("B"), Slot.from("A"));
  expect(slot.resolve(byPosition)).toEqual([B1, A1, A2, A3, B2]);
});

test("Slot.centered puts the extra edge player on the left when the count is odd", () => {
  const slot = Slot.centered(Slot.from("A"), Slot.from("B"));
  expect(slot.resolve(byPosition)).toEqual([A1, A2, B1, B2, A3]);
});

test("Slot.centered with an empty middle returns just the edge players", () => {
  const slot = Slot.centered(Slot.from("A"), Slot.from("Z"));
  expect(slot.resolve(byPosition)).toEqual([A1, A2, A3]);
});

// ---------- createFormation ----------

const TEST_FORMATION = {
  rows: [
    { name: "front", slots: [Slot.pick("B", 1), Slot.pick("A")] },
    { name: "back", slots: [Slot.from("A", 1)] },
    { name: "empty", slots: [Slot.pick("Z")] },
  ],
};

test("createFormation fills each row from its slots, in definition order", () => {
  const rows = createFormation([A1, A2, A3, B1, B2], TEST_FORMATION);

  expect(rows.map((row) => row.name)).toEqual(["front", "back", "empty"]);
  expect(rows[0].players).toEqual([B2, A1]);
  expect(rows[1].players).toEqual([A2, A3]);
});

test("createFormation leaves out missing players and keeps a row with none empty", () => {
  const rows = createFormation([A1], TEST_FORMATION);

  expect(rows[0].players).toEqual([A1]);
  expect(rows[1].players).toEqual([]);
  expect(rows[2].players).toEqual([]);
});

test("createFormation groups players in their original order within a position", () => {
  const rows = createFormation([A3, A1, A2], {
    rows: [{ name: "only", slots: [Slot.from("A")] }],
  });

  expect(rows[0].players).toEqual([A3, A1, A2]);
});
