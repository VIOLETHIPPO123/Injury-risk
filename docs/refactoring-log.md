# Refactoring Log

Code smells found in the SIDELINED backend and the refactoring used to fix each one.
Each entry shows the code before and after the change and links the commit that made it.
Every refactor is its own commit, and the full backend test suite passed after each one.

| #   | Smell          | Category     | Where                                                | Refactoring        | Commit                                                                                                     |
| --- | -------------- | ------------ | ---------------------------------------------------- | ------------------ | ---------------------------------------------------------------------------------------------------------- |
| 1   | Duplicate Code | Dispensables | `InMemoryTeamRepository`, `InMemoryPlayerRepository` | Extract Superclass | [`466fb6b`](https://github.com/VIOLETHIPPO123/Injury-risk/commit/466fb6b4891def52f1d7d1169e984de5a3a24ed2) |

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

## 2. Primitive Obsession: team and position as strings (DEV-XX)

**Smell:** Primitive Obsession (Bloaters)
**Refactoring:** Replace Type Code with Class (a Java enum for each field)
**Commit:** [`4eb07fb`]
(https://github.com/VIOLETHIPPO123/Injury-risk/commit/4eb07fb10ede8e18f3b63231fe89758cbba2324b)

### What was wrong

`Player` stored its team and its position as `String`, although each has a small, fixed
set of valid values: 32 team codes and 22 positions. To the compiler, `"ARI"` and
`"AR1"` are equally good strings, so a typo in any of the 940 player rows compiled and
ran. The valid teams were listed in `InMemoryTeamRepository`. The valid positions were not
listed anywhere; they were only implied by the data, the tests, and the section comments
in `InMemoryPlayerRepository`.

Before: `Player.java`

```java
public class Player {

    private Long id;
    private String name;
    private String team;        // "ARI", "KC", ...
    private String position;    // "QB", "WR", ...
    private int snapsLastGame;
    private int snapsLast4Games;

    public Player(Long id, String name, String team, String position,
                  int snapsLastGame, int snapsLast4Games) { ... }

    public String getTeam()     { return team; }
    public String getPosition() { return position; }
    // ...other getters unchanged
}
```

Before: `InMemoryPlayerRepository.java`. The 940 player rows are shortened to one.

```java
new Player(1L, "Jacoby Brissett", "ARI", "QB", 68, 253),
// ...the other 939 players, unchanged by this refactor
```

Before: `InMemoryPlayerRepositoryTest.java`. This test was the only check that a player's team
was real.

```java
@Test
void findAll_everyPlayerBelongsToAKnownTeam() {
    Set<String> teamCodes = new InMemoryTeamRepository().findAll().stream()
        .map(Team::getCode)
        .collect(Collectors.toSet());

    for (Player player : playerRepository.findAll()) {
        assertTrue(teamCodes.contains(player.getTeam()),
            player.getName() + " is on unknown team " + player.getTeam());
    }
}
```

### Why it was a problem

- **Typos compile and run.** `new Player(1L, "Jacoby Brissett", "AR1", "QBB", 68, 253)`
  is valid Java. Arizona would quietly lose its quarterback, and the only warning would
  be a failing lineup test whose message ("ARI should have exactly one QB") doesn't point
  at the typo.
- **The valid values lived in the wrong places.** Teams were in another class, positions
  were nowhere, and the offense/defense/special-teams grouping was a set of comments.
- **String comparisons can silently match nothing.** `positionCounts.getOrDefault("WR", 0L)`
  returns 0 for a misspelled key, so a test could pass or fail for the wrong reason.

### The fix

Two enums, one per field. The team enum is called `NflTeam` because a `Team` class
already exists for the `/teams` response.

After: `NflTeam.java` (new)

```java
public enum NflTeam {
    ARI("Arizona Cardinals"),
    // ...the other 31 teams, in the same order as before
    WAS("Washington Commanders");

    private final String displayName;

    NflTeam(String displayName) {
        this.displayName = displayName;
    }

    public String getCode()        { return name(); }
    public String getDisplayName() { return displayName; }
}
```

After: `Position.java` (new, complete)

```java
public enum Position {
    // Offense
    QB(Group.OFFENSE), RB(Group.OFFENSE), FB(Group.OFFENSE), WR(Group.OFFENSE),
    TE(Group.OFFENSE), LT(Group.OFFENSE), LG(Group.OFFENSE), C(Group.OFFENSE),
    RG(Group.OFFENSE), RT(Group.OFFENSE),

    // Defense
    DE(Group.DEFENSE), DT(Group.DEFENSE), LB(Group.DEFENSE),
    CB(Group.DEFENSE), SS(Group.DEFENSE), FS(Group.DEFENSE),

    // Special teams
    PK(Group.SPECIAL_TEAMS), P(Group.SPECIAL_TEAMS), H(Group.SPECIAL_TEAMS),
    PR(Group.SPECIAL_TEAMS), KR(Group.SPECIAL_TEAMS), LS(Group.SPECIAL_TEAMS);

    public enum Group { OFFENSE, DEFENSE, SPECIAL_TEAMS }

    private final Group group;

    Position(Group group) {
        this.group = group;
    }

    public Group getGroup() { return group; }
}
```

- The constant names are the codes, so there is no second string to keep in sync.
- `Group` replaces the section comments with data the code can use
  (`position.getGroup()`).

After: `Player.java` (changed lines only)

```java
private NflTeam team;
private Position position;

public Player(Long id, String name, NflTeam team, Position position,
              int snapsLastGame, int snapsLast4Games) { ... }

public NflTeam getTeam()      { return team; }
public Position getPosition() { return position; }
```

After: `InMemoryPlayerRepository.java`. The rows lost their quotes (two find-and-replace
passes, one per field), and the class gained two static imports.

```java
import static com.sidelined.backend.model.NflTeam.*;
import static com.sidelined.backend.model.Position.*;

new Player(1L, "Jacoby Brissett", ARI, QB, 68, 253),
// ...the other 939 players, same change
```

The same typo as above, `new Player(1L, "Jacoby Brissett", AR1, QBB, 68, 253)`, is now a
compile error on that line (`cannot find symbol`).

After: `InMemoryTeamRepository.java`. The team list is now built from the enum, so the 32
teams are written once instead of twice.

```java
private static final List<Team> TEAMS = Arrays.stream(NflTeam.values())
    .map(t -> new Team(t.getCode(), t.getDisplayName()))
    .toList();

public InMemoryTeamRepository() {
    super(TEAMS, Team::getCode);
}
```

After: `InMemoryPlayerRepositoryTest.java`

- `findAll_everyPlayerBelongsToAKnownTeam` was **deleted**. The compiler now guarantees
  every player's team is one of the 32 enum values, and the team list is built from that
  same enum, so the test can no longer fail.
- The three lineup tests were retyped. The offensive one is shown; the defensive and
  special-teams ones follow the same pattern.

```java
List<Position> singlePositions = List.of(
    Position.QB, Position.RB, Position.TE, Position.LT,
    Position.LG, Position.C, Position.RG, Position.RT);

Map<NflTeam, List<Player>> playersByTeam = playerRepository.findAll().stream()
    .collect(Collectors.groupingBy(Player::getTeam));

for (Map.Entry<NflTeam, List<Player>> team : playersByTeam.entrySet()) {
    Map<Position, Long> positionCounts = team.getValue().stream()
        .collect(Collectors.groupingBy(Player::getPosition, Collectors.counting()));

    for (Position position : singlePositions) {
        assertEquals(1L, positionCounts.getOrDefault(position, 0L),
            team.getKey() + " should have exactly one " + position);
    }
    assertEquals(3L, positionCounts.getOrDefault(Position.WR, 0L),
        team.getKey() + " should have three WRs");
}
```

### What stayed the same

- `Team` and the `/teams` response are unchanged. It still returns a code and a name for
  each team.
- `GET /players` still returns `"team": "ARI"` and `"position": "QB"`, because Jackson
  writes an enum as its name.
- `PlayerService`, `PlayerController` and `InMemoryTeamRepositoryTest` needed no edits,
  because none of them construct a `Player` or call `getTeam()`.

### Verification

- **Tests:** `./mvnw test` passes: **TODO** tests, 0 failures. (Entry 1 ended at 43; one
  test was deleted, plus any new ones you added.)
- **API unchanged:** **TODO** run the app before and after and diff `GET /players` and
  `GET /teams`. State the result here (for example, "byte-for-byte identical JSON,
  940 players, 32 teams, same order").
- **Compile check:** every one of the 940 rows now has to name a real team and position
  to compile, which the old string rows could not guarantee.

### Not changed here

- `Team` and `NflTeam` overlap (both describe a team). Consolidating them, by returning the
  enum from the API with `@JsonFormat(shape = OBJECT)` and deleting `Team`, would change
  the API's design, not just remove a primitive, so it was left for a separate refactor.
- The three lineup tests now differ only in which positions they check. They could share
  a helper method; that is a Duplicate Code fix, not part of this one.
- The front end was not checked for assumptions about `team` and `position`. The JSON
  values are the same strings, so it should be unaffected.
