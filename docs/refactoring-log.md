# Refactoring Log

Code smells found in the SIDELINED backend and the refactoring used to fix each one.
Each entry shows the code before and after the change and links the commit that made it.
Every refactor is its own commit, and the full backend test suite passed after each one.

| # | Smell | Category | Where | Refactoring | Commit |
|---|---|---|---|---|---|
| 1 | Duplicate Code | Dispensables | `InMemoryTeamRepository`, `InMemoryPlayerRepository` | Extract Superclass | [`466fb6b`](https://github.com/VIOLETHIPPO123/Injury-risk/commit/466fb6b4891def52f1d7d1169e984de5a3a24ed2) |
| 2 | Data Clumps | Bloaters | `Player`, `InMemoryPlayerRepository` | Introduce Parameter Object (Extract Class) | [`b93857d`](https://github.com/VIOLETHIPPO123/Injury-risk/commit/b93857dcce86733b0149e9a814539e9c381a021a) |

<!-- To add an entry: add a row above, then a section below with the same headings. -->

## 1. Duplicate Code: in-memory repository lookups (DEV-83)

**Smell:** Duplicate Code (Dispensables)
**Refactoring:** Extract Superclass (Pull Up Field + Pull Up Method)
**Commit:** [`466fb6b`](https://github.com/VIOLETHIPPO123/Injury-risk/commit/466fb6b4891def52f1d7d1169e984de5a3a24ed2)

### What was wrong

`InMemoryTeamRepository` and `InMemoryPlayerRepository` each held hard-coded data and
then repeated the same lookup code underneath it. Running `diff` on the two lookup
blocks showed they differed only in type names (`Team`/`Player`, `String`/`Long`) and
the map's name (`byCode`/`byId`). The comments were copied word for word too.

Before: `InMemoryTeamRepository.java`. The 32 team rows are shortened to one, since
they are data and the refactor didn't change them.

```java
@Repository
public class InMemoryTeamRepository implements TeamRepository {

    private static final List<Team> TEAMS = List.of(
        new Team("ARI", "Arizona Cardinals"),
        // ...the other 31 teams, unchanged by this refactor
    );

    // Lookups by code without scanning the whole list. Collectors.toMap also fails fast on a duplicate.
    private final Map<String, Team> byCode = TEAMS.stream()
        .collect(Collectors.toMap(Team::getCode, Function.identity()));

    // The original order, so GET responses are unchanged.
    @Override
    public List<Team> findAll() {
        return TEAMS;
    }

    @Override
    public Optional<Team> findById(String id) {
        return Optional.ofNullable(byCode.get(id));
    }

    @Override
    public boolean existsById(String id) {
        return byCode.containsKey(id);
    }
}
```

Before: `InMemoryPlayerRepository.java`. The 940 player rows are shortened to one, as
above.

```java
@Repository
public class InMemoryPlayerRepository implements PlayerRepository {

    private static final List<Player> PLAYERS = List.of(
        new Player(1L, "Jacoby Brissett", "ARI", "QB", 68, 253),
        // ...the other 939 players, unchanged by this refactor
    );

    // Lookups by id without scanning the whole list. Collectors.toMap also fails fast on a duplicate.
    private final Map<Long, Player> byId = PLAYERS.stream()
        .collect(Collectors.toMap(Player::getId, Function.identity()));

    // The original order, so GET responses are unchanged.
    @Override
    public List<Player> findAll() {
        return PLAYERS;
    }

    @Override
    public Optional<Player> findById(Long id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public boolean existsById(Long id) {
        return byId.containsKey(id);
    }
}
```

### Why it was a problem

- **One change, two places.** Any change to how lookups work (for example, having
  `findAll()` return a copy instead of the internal list) had to be made by hand in both
  files. Nothing warned you if one was missed, and the two repositories would then quietly
  behave differently.
- **The comments went stale together.** The copied comments described history, not the
  code. "The original order, so GET responses are unchanged" only made sense if you knew
  about an earlier refactor. The player repository's comment also said "Starting
  offensive lineups," although the list had since gained defense (DEV-70) and special
  teams (DEV-71).
- **A third copy.** The same `findById`/`existsById` bodies also appear in
  `InMemoryWatchlistRepository` (see "Not changed here").

### The fix

The shared code moved up into one new abstract class. It is a class, not an interface,
because it has to hold the lookup map as a field.

After: `InMemoryReadOnlyRepository.java` (new, complete)

```java
// Shared lookups for read-only data held in memory. Subclasses supply the data
// and say which field is the id; the lookup code lives here once.
public abstract class InMemoryReadOnlyRepository<T, ID>
        implements ReadOnlyRepository<T, ID> {

    private final List<T> items;
    // Lookups by id without scanning the whole list.
    // Collectors.toMap also fails fast on a duplicate.
    private final Map<ID, T> byId;

    protected InMemoryReadOnlyRepository(List<T> items, Function<T, ID> idOf) {
        this.items = items;
        this.byId = items.stream()
            .collect(Collectors.toMap(idOf, Function.identity()));
    }

    // In the order the subclass listed them.
    @Override
    public List<T> findAll() {
        return items;
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public boolean existsById(ID id) {
        return byId.containsKey(id);
    }
}
```

- `T` is the kind of item stored (`Team` or `Player`), and `ID` is the type of its id
  (`String` or `Long`).
- `idOf` tells the base class which field is the id: `Team::getCode` or `Player::getId`.
  That one argument is the only real difference between the two old copies.

After: the two repositories now hold only their data and one constructor line.

```java
@Repository
public class InMemoryTeamRepository
        extends InMemoryReadOnlyRepository<Team, String>
        implements TeamRepository {

    private static final List<Team> TEAMS = List.of(
        new Team("ARI", "Arizona Cardinals"),
        // ...the other 31 teams, unchanged by this refactor
    );

    public InMemoryTeamRepository() {
        super(TEAMS, Team::getCode);
    }
}
```

```java
@Repository
public class InMemoryPlayerRepository
        extends InMemoryReadOnlyRepository<Player, Long>
        implements PlayerRepository {

    private static final List<Player> PLAYERS = List.of(
        new Player(1L, "Jacoby Brissett", "ARI", "QB", 68, 253),
        // ...the other 939 players, unchanged by this refactor
    );

    public InMemoryPlayerRepository() {
        super(PLAYERS, Player::getId);
    }
}
```

The outdated comments were fixed at the same time: each class comment now says what the
class does, and the player list's comment names offense, defense and special teams.

### What stayed the same

- `ReadOnlyRepository`, `TeamRepository` and `PlayerRepository` are unchanged, so
  nothing that uses them (services, `PlayerExistsValidator`, controllers) changed.
- `@Repository` stays on the two concrete classes only, so Spring injects them exactly as
  before.
- When the external API arrives, its repository can implement `PlayerRepository`
  directly; the in-memory base class doesn't stand in the way.

### Verification

- **Tests:** `./mvnw test` passes: 43 tests, 0 failures. The 38 existing tests passed
  without changes. 5 new tests in `InMemoryReadOnlyRepositoryTest` cover the shared code
  directly: `findAll` order, `findById` hit and miss, `existsById`, and that a
  duplicate id still fails at startup.
- **API unchanged:** the app was run before and after the change, and `GET /players` and
  `GET /teams` returned byte-for-byte identical JSON (all 940 players, all 32 teams, same
  order).
- **Size:** the two repositories lost 52 lines of duplicated code and imports and gained 17;
  the new shared class is 40 lines. The total is 5 lines longer (52 removed, 57 added),
  but the lookup code now exists once instead of twice.

### Not changed here

`InMemoryWatchlistRepository` repeats the `findById`/`existsById` bodies, but it is a
different kind of repository: writable, with a map that changes and ids it assigns
itself. Making it share this base class would mean changing its design, not just
removing a copy, so it was left as is.

## 2. Data Clumps: the snap counts in `Player` (DEV-84)

**Smell:** Data Clumps (Bloaters)
**Refactoring:** Introduce Parameter Object (Extract Class)
**Commit:** [`b93857d`](https://github.com/VIOLETHIPPO123/Injury-risk/commit/b93857dcce86733b0149e9a814539e9c381a021a)

### What was wrong

`snapsLastGame` and `snapsLast4Games` always appeared together: two fields, two
constructor parameters, two getters, and a pair of numbers at the end of each of the 940
rows in `InMemoryPlayerRepository`. Neither number means anything alone. They are the
two inputs to the injury-risk (ACWR) score, and every place in the frontend that reads
them passes both to `calculateAcwr` together.

Before: `Player.java`

```java
public class Player {

    private Long id;
    private String name;
    private String team;
    private String position;
    private int snapsLastGame;
    private int snapsLast4Games;

    public Player(Long id, String name, String team, String position, int snapsLastGame, int snapsLast4Games) {
        this.id = id;
        this.name = name;
        this.team = team;
        this.position = position;
        this.snapsLastGame = snapsLastGame;
        this.snapsLast4Games = snapsLast4Games;
    }

    // ...getters for id, name, team and position, unchanged by this refactor
    public int getSnapsLastGame() { return snapsLastGame; }
    public int getSnapsLast4Games() { return snapsLast4Games; }
}
```

Before: one of the 940 rows in `InMemoryPlayerRepository.java`

```java
new Player(1L, "Jacoby Brissett", "ARI", "QB", 68, 253),
```

Before: the only place the rule linking the two numbers was written down,
`InMemoryPlayerRepositoryTest.java`

```java
@Test
void findAll_snapCountsAreValidForAcwr() {
    List<Player> players = playerRepository.findAll();

    // Assert — the last game is one of the last four, so it can't exceed their total.
    for (Player player : players) {
        assertTrue(player.getSnapsLastGame() >= 0, player.getName() + " has negative snaps");
        assertTrue(player.getSnapsLastGame() <= player.getSnapsLast4Games(),
            player.getName() + " has more snaps last game than in the last four games");
    }
}
```

### Why it was a problem

- **The rule had no home in the code.** Snaps can't be negative, and the last game is
  one of the last four, so its snaps can't exceed their total. That rule existed only in
  a test. `new Player(..., 80, 50)` compiled and ran; bad data was caught only if the
  tests happened to run against it.
- **Easy to swap.** The two `int`s sat side by side at the end of a six-parameter
  constructor. Swapping them still compiles and quietly turns every risk score upside
  down, and with 940 rows a mistake like that is hard to spot.
- **No place for more workload data.** Practice snaps or a season total would have
  become more loose `int` fields on `Player`, with `Player`'s constructor (and all 940
  rows) changing each time.

### The fix

The two numbers moved into one new record, and the rule moved from the test into the
record's constructor, so a `Workload` that breaks it can't be created.

After: `Workload.java` (new, complete)

```java
// The snap counts that feed the injury-risk (ACWR) score. They only mean something as a pair,
// so they travel together and the rule linking them is checked once, here.
public record Workload(int snapsLastGame, int snapsLast4Games) {

    public Workload {
        if (snapsLastGame < 0 || snapsLast4Games < 0) {
            throw new IllegalArgumentException("Snap counts cannot be negative: "
                + snapsLastGame + ", " + snapsLast4Games);
        }
        // The last game is one of the last four, so it can't exceed their total.
        if (snapsLastGame > snapsLast4Games) {
            throw new IllegalArgumentException("Snaps last game (" + snapsLastGame
                + ") exceed snaps in the last four games (" + snapsLast4Games + ")");
        }
    }
}
```

After: `Player.java`

```java
// The snap getters have no field of their own, so Jackson would list them last and swap their
// order. Pinning the order keeps GET /players byte-for-byte what it was before Workload existed.
@JsonPropertyOrder({"id", "name", "team", "position", "snapsLastGame", "snapsLast4Games"})
public class Player {

    private Long id;
    private String name;
    private String team;
    private String position;
    private Workload workload;

    public Player(Long id, String name, String team, String position, Workload workload) {
        this.id = id;
        this.name = name;
        this.team = team;
        this.position = position;
        this.workload = workload;
    }

    // ...getters for id, name, team and position, unchanged by this refactor
    // Read through to the workload so the JSON keeps its flat snapsLastGame/snapsLast4Games fields.
    public int getSnapsLastGame() { return workload.snapsLastGame(); }
    public int getSnapsLast4Games() { return workload.snapsLast4Games(); }
}
```

After: the same row (the other 939 rows changed the same way)

```java
new Player(1L, "Jacoby Brissett", "ARI", "QB", new Workload(68, 253)),
```

- The 940 rows were changed with one regex find-and-replace, then checked: all 940 rows
  use `new Workload(...)`, each row's id, name, team, position and both numbers are
  unchanged, and every other line in the file is byte-for-byte the same.
- `Player` keeps `getSnapsLastGame()` and `getSnapsLast4Games()`, so the JSON stays flat
  and the frontend needed no change. There is deliberately no `getWorkload()`: nothing
  would call it, and without `@JsonIgnore` Jackson would add a nested
  `"workload": {...}` object to the API.
- `@JsonPropertyOrder` is needed because Jackson lists properties backed by a field
  before getter-only ones. Without it, the values were identical but each player's JSON
  listed `snapsLast4Games` before `snapsLastGame`.
- The data test `findAll_snapCountsAreValidForAcwr` was removed because it could no
  longer fail. A row that broke the rule now throws while the player list is built, so
  the app would fail to start and every test that touches the repository would fail
  first. The rule itself is now tested directly in `WorkloadTest`.

### What stayed the same

- `PlayerRepository`, `PlayerService`, `PlayerController` and `PlayerExistsValidator`
  are unchanged.
- `GET /players` returns the same JSON, so the frontend is untouched.
- No ACWR or average methods were added to `Workload`. The frontend computes those
  today, and backend methods that nothing calls would be a new smell (Speculative
  Generality). They can move into `Workload` when the backend needs them.

### Verification

- **Tests:** `./mvnw test` passes: 47 tests, 0 failures. Of the 43 tests before this
  change, 42 still pass (`PlayerServiceTest` now builds its `Player` with a `Workload`),
  and 1 was removed (see above). 5 new tests in `WorkloadTest` cover a valid pair, the
  edge cases `(0, 0)` and `(60, 60)`, negative snaps in either field, and more snaps
  last game than in the last four.
- **Data passes the new rule:** the app starts, which means all 940 rows built a valid
  `Workload`.
- **API unchanged:** the app was run before and after the change, and `GET /players`
  returned byte-for-byte identical JSON (97,221 bytes, all 940 players, same order).
- **Frontend:** no frontend files changed.

### Not changed here

`team` is still a `String`, and `position` is still a `String` holding one of 22 fixed
values. That is a separate smell (Primitive Obsession) with its own fix, so it was left
out to keep this refactor to one smell.
