# Refactoring Log

Code smells found in the SIDELINED backend and the refactoring used to fix each one.
Each entry shows the code before and after the change and links the commit that made it.
Every refactor is its own commit, and the full backend test suite passed after each one.

| # | Smell | Category | Where | Refactoring | Commit |
|---|---|---|---|---|---|
| 1 | Duplicate Code | Dispensables | `InMemoryTeamRepository`, `InMemoryPlayerRepository` | Extract Superclass | [`466fb6b`](https://github.com/VIOLETHIPPO123/Injury-risk/commit/466fb6b4891def52f1d7d1169e984de5a3a24ed2) |

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
