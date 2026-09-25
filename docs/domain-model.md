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

Currently backed by a hardcoded in-memory list in `PlayerService`
(`GET /players`), explicitly commented as illustrative placeholder data,
not real game data.

### Team — Implemented

`backend/.../model/Team.java`

| Attribute | Type | Notes |
|---|---|---|
| code | String | e.g. `"ARI"` — the join key `Player.team` matches against |
| name | String | e.g. `"Arizona Cardinals"` |

Hardcoded list of all 32 NFL teams in `TeamService` (`GET /teams`).

### WatchlistEntry — Implemented

`backend/.../model/WatchlistEntry.java`

| Attribute | Type | Notes |
|---|---|---|
| id | Long | Assigned by the repository |
| playerId | Long | References `Player.id`; validated to exist on create |
| note | String | Free text; the only field that's editable after creation |

Persisted in `WatchlistRepository`, an in-memory `ConcurrentHashMap`
(comment confirms: "until a database is wired in. Data resets when the
backend restarts"). Full CRUD exists except a list/`GET` endpoint. No
`userId` field — this is currently a single global watchlist, not
per-user (see **User**, below).

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

### Injury — Planned

Named in this ticket's scope and in DEV-55's data-source disclosure
("public snap-count **and injury data**"). Not modeled anywhere in the
codebase yet — no fields, no endpoint. Likely future shape: `player` (ref),
`type`/`description`, `dateReported`, `status` (active/recovering/
recovered), `source`.

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
current implementation: today's `WatchlistEntry` is a single global list
with no owner, so there's no actual concept yet of "my roster" vs. "the
full player pool." Multi-league support is explicitly out of scope for
this semester's MVP, but even single-league, single-user support implies
at least a `User` and a `Roster` (a User's chosen subset of Players)
eventually need to exist.

## Relationships

| From | To | Multiplicity | Notes |
|---|---|---|---|
| Team | Player | 1 → * | By matching `Player.team` (string code) to `Team.code` — a soft/logical reference, not an enforced foreign key (no database yet) |
| Player | WatchlistEntry | 1 → * | Via `WatchlistEntry.playerId`; validated against `PlayerService` on create, but not a real FK constraint |
| Division/Conference | Team | 1 → * | Static grouping only, defined in frontend data, not derivable from `Team` itself |
| Player | WorkloadRatio | 1 → 1 (derived) | Computed per player per request from that player's two snap-count fields |
| WorkloadRatio | RiskLevel | 1 → 1 (derived) | Pure function of the ratio value; no independent identity |
| *(planned)* Player | WorkloadEntry | 1 → * | A player would have many workload entries (one per game/week) |
| *(planned)* Player | Injury | 1 → * | A player could have multiple injury records over time |
| *(planned)* Player | RiskScore | 1 → * | If persisted, a player would accumulate a history of scores over time (one per computation), not just one current value |
| *(planned)* User | Roster | 1 → * or 1 → 1 | Depends on whether multi-roster-per-user is ever needed; MVP likely only needs 1 → 1 |
| *(planned)* Roster | Player | * → * | A roster holds many players; a player (league-wide pool) isn't exclusive to one roster's *data*, though typically exclusive to one manager's *claim* within a league |
| *(planned)* League | User | 1 → * | A league has many member managers |

No inheritance exists anywhere in the current model — all classes are
flat, unrelated data holders (no shared base class, no polymorphism).

## Summary for diagramming

- **Solid, ready to diagram as-is:** `Player`, `Team`, `WatchlistEntry`,
  and their two real relationships.
- **Worth showing as derived/computed nodes** (dashed, not persisted):
  `WorkloadRatio`, `RiskLevel`, `Division`/`Conference`.
- **Worth sketching as planned/future classes** (clearly marked distinct
  from the implemented ones): `WorkloadEntry`, `Injury`, a persisted
  `RiskScore`, and the `User`/`League`/`Roster` cluster — the last of
  which represents the largest gap between the product vision in
  `docs/requirements.md` and what's actually implemented today.
