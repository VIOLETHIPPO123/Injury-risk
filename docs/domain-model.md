# Domain Model

Reference for the project's domain classes and their relationships, as a
basis for diagramming (class/ER diagrams). Reflects the actual current
implementation, not just the intended design — several classes described in
the requirements and personas don't exist in code yet, and are called out
below as planned.

Each class is tagged:

- **Implemented** — a real class in the codebase today.
- **Derived** — computed at request/render time, never persisted as its own
  class.
- **Planned** — named in requirements/personas or ticket descriptions, but
  not modeled in code yet.

## Classes

### Player — Implemented

`backend/.../model/Player.java`

| Attribute | Type | Notes |
|---|---|---|
| id | Long | |
| name | String | |
| team | String | Team code (e.g. `"ARI"`), not an object reference |
| position | String | e.g. `QB`, `RB`, `LT` — free-form, not an enum |
| snapsLastGame | int | Most recent game's snap count |
| snapsLast4Games | int | Total snaps over the last 4 games (used as the "chronic" workload) |

Read through the read-only `PlayerRepository` interface. Today's
implementation, `InMemoryPlayerRepository`, is a hardcoded list served by
`PlayerService` (`GET /players`), explicitly commented as illustrative
placeholder data, not real game data. Planned: an external-API-backed
implementation.

### Team — Implemented

`backend/.../model/Team.java`

| Attribute | Type | Notes |
|---|---|---|
| code | String | e.g. `"ARI"` — the join key `Player.team` matches against |
| name | String | e.g. `"Arizona Cardinals"` |

Read through the read-only `TeamRepository` interface, keyed by `code`.
Today's implementation, `InMemoryTeamRepository`, is a hardcoded list of all
32 NFL teams, served by `TeamService` (`GET /teams`). Planned: an
external-API-backed implementation.

### RosterEntry — Implemented

`backend/.../model/RosterEntry.java` (was `WatchlistEntry` before DEV-86)

| Attribute | Type | Notes |
|---|---|---|
| id | Long | Assigned by the repository |
| playerId | Long | References `Player.id`; validated to exist on create |
| slot | String | A starting slot of the league's format (a `RosterSlot` name, e.g. `"RB"`), `BENCH` or `IR`. New players start on `BENCH` |
| note | String | Free text; editable after creation |

Served at `/roster` (CRUD, plus `PUT /roster/{id}/slot` to move a player).
Persisted through the `RosterEntryRepository` interface. Today's
implementation, `InMemoryRosterEntryRepository`, is an in-memory
`ConcurrentHashMap` (data resets when the backend restarts). The rules live
in one validator class each: `RosterEntryValidator`s for adding (player
exists, fantasy-eligible position, not already rostered, roster not full),
`SlotMoveValidator`s for moving (slot allows the position, slot has room, IR
needs an Active injury, leaving IR needs a free spot), and
`FormatChangeValidator`s for switching formats (the roster fits the new
maximum). Switching formats moves every player to the bench. No `userId`
field — this is a single global roster, not per-user (see **User**, below).

### WorkloadRatio (ACWR) — Derived

`frontend/.../utils/acwr.js` → `calculateAcwr()`

Computed on the fly from a `Player`'s `snapsLastGame` and
`snapsLast4Games` (`acute ÷ chronic`, where chronic = `snapsLast4Games / 4`).
Never stored; recalculated every render. Not a backend concept at all today.

### RiskLevel — Derived

`frontend/.../utils/acwr.js` → `riskLevel()`

Computed from a `WorkloadRatio` value into `{ label, tier }`, where
`tier` is one of `low` / `medium` / `high` / `unknown`. Purely a
presentation-layer classification, not persisted or exposed by the API.

### Division / Conference — Derived (static lookup)

`frontend/.../data/divisions.js`

A static, hardcoded grouping of `Team` codes into two conferences (AFC/NFC)
× four divisions each (East/North/South/West). Not a queryable class or
API concept — just a lookup table the frontend uses for organization.

### WorkloadEntry — Planned

Named explicitly in this ticket's scope ("workload entries") but not
modeled today. `Player` currently stores only two flat snap-count ints
rather than a per-game history. A real `WorkloadEntry` would likely need:
`player` (ref), `gameDate`/`week`, `snaps`, `season` — enabling real ACWR
calculation from actual per-game data instead of two rolling ints, and
enabling the deferred "historical risk-score trend charts" feature.

### Injury — Implemented

`backend/.../model/Injury.java`, status in `backend/.../model/InjuryStatus.java`

| Attribute | Type | Notes |
|---|---|---|
| id | Long | Unique per injury |
| playerId | Long | References `Player.id` |
| season | int | e.g. 2026 |
| week | int | 1–18 |
| bodyPart | String | e.g. `"Hamstring"` |
| type | String | e.g. `"Strain"` |
| gamesMissed | int | 0 or more |
| status | InjuryStatus | `Active`, `Recovering` or `Recovered` (JSON uses those labels) |

Read through the read-only `InjuryRepository` interface, keyed by `id`, which
also looks up a player's injuries (`findByPlayerId`) and answers "does this
player have an Active injury?" (`hasActiveInjury`, which the IR rule uses).
Today's implementation, `InMemoryInjuryRepository`, is made-up sample data
for real players. Served by `InjuryService` at `GET /players/{id}/injuries`,
newest first; an unknown player is a 404, a player with no injuries an empty
list. Planned: an external-API-backed implementation.

### RiskScore — Planned (as a first-class, persisted concept)

Currently only exists as the **derived** `RiskLevel` above — computed
fresh every time, never stored or timestamped. Would need to become a
real persisted class (`player` ref, `score`, `computedAt`) to support two
explicitly-deferred-but-planned features from `docs/requirements.md`:
"risk-score alerts" and "historical risk-score trend charts" — both
require comparing a score against its own past values over time, which a
purely-derived value can't do.

### User / League / Roster — Planned (not modeled at all)

The personas and user stories throughout `docs/requirements.md` describe
a fantasy manager with **their own roster** inside a **league** of other
managers — but no `User`, `League`, or `Roster` class exists anywhere in
the code. This is the biggest gap between the described domain and the
current implementation: today's `RosterEntry` list is a single global roster
with no owner, so there's no concept yet of one manager's roster vs.
another's. Multi-league support is explicitly out of scope for
this semester's MVP, but even single-league, single-user support implies
at least a `User` and a `Roster` (a User's chosen subset of Players)
eventually need to exist.

## Relationships

| From | To | Multiplicity | Notes |
|---|---|---|---|
| Team | Player | 1 → * | By matching `Player.team` (string code) to `Team.code` — a soft/logical reference, not an enforced foreign key (no database yet) |
| Player | RosterEntry | 1 → 0..1 | Via `RosterEntry.playerId`; validated against `PlayerRepository` on create (and a player can be rostered once), but not a real FK constraint |
| Division/Conference | Team | 1 → * | Static grouping only, defined in frontend data, not derivable from `Team` itself |
| Player | WorkloadRatio | 1 → 1 (derived) | Computed per player per request from that player's two snap-count fields |
| WorkloadRatio | RiskLevel | 1 → 1 (derived) | Pure function of the ratio value; no independent identity |
| *(planned)* Player | WorkloadEntry | 1 → * | A player would have many workload entries (one per game/week) |
| Player | Injury | 1 → * | Via `Injury.playerId`; a player can have many injuries over time |
| *(planned)* Player | RiskScore | 1 → * | If persisted, a player would accumulate a history of scores over time (one per computation), not just one current value |
| *(planned)* User | Roster | 1 → * or 1 → 1 | Depends on whether multi-roster-per-user is ever needed; MVP likely only needs 1 → 1 |
| *(planned)* Roster | Player | * → * | A roster holds many players; a player (league-wide pool) isn't exclusive to one roster's *data*, though typically exclusive to one manager's *claim* within a league |
| *(planned)* League | User | 1 → * | A league has many member managers |

No inheritance exists anywhere in the current model — all classes are
flat, unrelated data holders (no shared base class, no polymorphism).

## Summary for diagramming

- **Solid, ready to diagram as-is:** `Player`, `Team`, `RosterEntry`,
  and their two real relationships.
- **Worth showing as derived/computed nodes** (dashed, not persisted):
  `WorkloadRatio`, `RiskLevel`, `Division`/`Conference`.
- **Worth sketching as planned/future classes** (clearly marked distinct
  from the implemented ones): `WorkloadEntry`, a persisted
  `RiskScore`, and the `User`/`League`/`Roster` cluster — the last of
  which represents the largest gap between the product vision in
  `docs/requirements.md` and what's actually implemented today.
