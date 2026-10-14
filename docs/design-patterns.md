# Design Patterns in the Formation Layouts

Write-up of DEV-80: applying the **Strategy** and **Factory** patterns to the
team roster page's formation layouts (`frontend/src/utils/formation.js`). The
"before" code is `main` as of commit `17d52e5`; the "after" code is the DEV-80
branch.

| Pattern | Applied to | What it does here |
|---|---|---|
| Strategy | Formation slots | Each slot carries its own rule for picking players, so one `createFormation` handles every unit |
| Factory (static factory methods) | Creating slots | `Slot.pick`, `Slot.from` and `Slot.centered` are the only code that creates slots |

Strategy fixes the Open/Closed problem below; Factory doesn't — it keeps
slot creation in one place so the definitions stay simple. The two patterns
answer different questions: Strategy is about **behavior** (how a slot picks
players), Factory about **creation** (how slot objects get made).

## The problem

Each unit's layout was a hard-coded function. DEV-72 and DEV-73 added defense
and special teams by copying the offense function, so `formation.js` had three:

```js
function createFormation(teamPlayers) {           // offense
  const byPosition = groupByPosition(teamPlayers);
  return {
    topRow: [byPosition.LT?.[0], byPosition.LG?.[0], byPosition.C?.[0], /* ... */].filter(Boolean),
    bottomRow: [byPosition.WR?.[0], byPosition.WR?.[1], byPosition.RB?.[0], /* ... */].filter(Boolean),
  };
}
function createDefenseFormation(teamPlayers) { /* same shape, different positions */ }
function createSpecialTeamsFormation(teamPlayers) { /* same shape again */ }
```

`TeamRosterPage` also named every row it could render (`topRow`, `middleRow`,
`bottomRow`). This broke the **Open/Closed Principle**: every new unit (a
goal-line offense, a dime defense) meant copying a function, exporting it,
importing it into the page — and a unit with a fourth row meant editing the
page as well. Working code had to be modified to add a layout.

## Strategy

### What it is here

A formation is now **data**: a list of named rows, each a list of slots.
Every slot is a strategy — an object with one method, `resolve(byPosition)`,
that returns the players it fills. `createFormation` asks each slot for its
players without knowing what kind of slot it is:

```js
function createFormation(players, formation) {
  const byPosition = groupByPosition(players);

  return formation.rows.map((row) => ({
    name: row.name,
    players: row.slots
      .flatMap((slot) => slot.resolve(byPosition))
      .filter(Boolean),
  }));
}
```

`createFormation` contains no position names and no checks on slot kind.
Adding a unit means adding a definition to `formationDefinitions.js` and one
line to `UNITS` in `TeamRosterPage.jsx` so it appears in the toggle; no layout
code changes, and the page renders whatever rows the definition has.

### Why Strategy fits better than the alternatives

- **Plain data slots** (`{ position: "LT", index: 0 }`) are enough for offense
  and special teams, but not defense, whose layout has rules: every linebacker
  however many there are, the line centered around the tackles, extra corners
  in the nickel spot. Data alone would push those rules into `createFormation`
  as `if (slot.type === "centered") … else if …` — a type switch, the same
  Open/Closed problem moved somewhere else. With Strategy, the rule lives in
  the slot, so a new kind of slot never touches `createFormation`.
- **Template Method** would mean an abstract formation with one subclass per
  unit, each overriding the rows. Every subclass would still write its own
  player-picking code — the duplication we're removing. The variation is per
  *slot*, not per *unit*, so composing slots fits better than inheriting
  formations.

## Factory

### What it is here

Slots are created through static factory methods, in the style the lecture
attributes to Joshua Bloch. Definitions call the factory and never build slot
objects themselves:

```js
const Slot = {
  pick: (position, nth = 0) => ({
    resolve: (byPosition) => [byPosition[position]?.[nth]],
  }),
  from: (position, start = 0) => ({
    resolve: (byPosition) => (byPosition[position] ?? []).slice(start),
  }),
  centered: (edges, middle) => ({
    resolve: (byPosition) =>
      centerBetween(edges.resolve(byPosition), middle.resolve(byPosition)),
  }),
};
```

The definitions read as a description of the lineup:

```js
const DEFENSE = {
  rows: [
    { name: "top-row", slots: [Slot.pick("CB", 0), Slot.from("CB", 2), Slot.pick("FS"), Slot.pick("SS"), Slot.pick("CB", 1)] },
    { name: "middle-row", slots: [Slot.from("LB")] },
    { name: "bottom-row", slots: [Slot.centered(Slot.from("DE"), Slot.from("DT"))] },
  ],
};
```

### Why Factory, and why it isn't the Open/Closed fix

The three definitions make 27 `Slot` calls. Without the factory, each would be a
hand-written `{ resolve: ... }` object, repeating the same lookup code and
exposing how slots work to every definition. The factory confines that to one
place — like the lecture's `ChannelFactory.create()` confining the `new`
calls. Adding a new kind of slot does mean editing `Slot`, and that's
deliberate: it's the one place that changes, rather than every definition.

## Cost — and why this isn't "patternitis"

The lecture's test for a pattern is whether the flexibility is actually
needed. Here it is: there are three kinds of slot and three units, the
duplication had already happened twice (DEV-72, DEV-73), and more units are
plausible. The cost is modest: no classes and one factory object, but more
lines overall — 133 across `formation.js` and `formationDefinitions.js`,
versus 91 before — because each definition lists its slots one per line. The
trade is more lines in exchange for no copied layout code: a new unit is a
new definition, not a new function.

A **Builder** (`formation().row(...).build()`), by contrast, *would* be
patternitis: the definitions are simple lists with no optional parts, so a
builder would add ceremony without adding flexibility.

## Verification

- All 14 existing `TeamRosterPage` tests pass **without changes**.
- The rendered HTML of every team × every unit (32 × 3 = 96 layouts, using
  the real 940-player data) was compared before and after: identical.
- New `formation.test.js` (12 tests) covers `createFormation` with a test-only
  definition using made-up positions, and each slot kind including missing
  players.
- Frontend: 61 tests pass; lint is clean.
