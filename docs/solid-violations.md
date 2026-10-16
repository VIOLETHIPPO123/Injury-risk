# SOLID Violations in WatchlistService

Write-up of three SOLID violations found in
`backend/.../service/WatchlistService.java` and how DEV-79 fixed them. The
"before" code is `main` as of commit `9585422`; the "after" code is the
DEV-79 branch.

| Principle | Where | Fixed by |
|---|---|---|
| Single Responsibility (SRP) | `ResponseStatusException` thrown in `createEntry`, `updateEntry`, `deleteEntry` | Domain exceptions + `ApiExceptionHandler` |
| Dependency Inversion (DIP) | The `watchlistRepository` and `playerService` fields and constructor | `WatchlistRepository` and `PlayerLookup` interfaces |
| Open/Closed (OCP) | The inline "player must exist" check in `createEntry` | `WatchlistEntryValidator` per rule |

The fixes build on each other: the OCP validator throws the SRP domain
exception and reaches players through the DIP interface.

## 1. Single Responsibility Principle

### What was wrong

`WatchlistService` decided both what counts as an error and how the web API
reports it:

```java
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
// ...
throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found");
```

The imports are the clue, not the violation itself: `HttpStatus` and
`ResponseStatusException` come from Spring's web packages, and a class that
only held watchlist rules would have no reason to know HTTP exists. The
violation is the decision those imports make possible — "send a 404" is an
HTTP decision, made inside the business-logic layer.

### Why it made changes risky

The class had two reasons to change, owned by different people:

1. **The watchlist rules** — e.g. "a user can watch at most 20 players."
2. **The HTTP contract** — e.g. "duplicates should be 409 Conflict," or
   "errors need a JSON body with a `code` field."

Concrete costs in this codebase:

- **HTTP decisions were split across two layers.** `WatchlistController`
  chose 201 and 204; `WatchlistService` chose 404. Finding every status code
  the API can return meant reading both.
- **Business-logic tests asserted HTTP.** `WatchlistServiceTest` checked
  `thrown.getStatusCode() == HttpStatus.NOT_FOUND`. Changing a status code
  would break tests of watchlist rules that hadn't changed.
- **No reuse outside a web request.** A scheduled job or import script calling
  `createEntry` would receive an HTTP 404 exception with no HTTP request in
  sight.

### The fix

The service now throws plain domain exceptions that say *what* went wrong:

```java
.orElseThrow(() -> new WatchlistEntryNotFoundException(id));
```

One `@RestControllerAdvice` in the controller layer decides *how* that becomes
an HTTP response:

```java
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({PlayerNotFoundException.class, WatchlistEntryNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(RuntimeException ex) {
        return Map.of("error", ex.getMessage());
    }
}
```

`WatchlistController` itself is unchanged — Spring applies the advice to every
controller automatically. All HTTP decisions now live in the controller layer,
and `WatchlistService` imports nothing from Spring Web. The API returns the
same status codes as before; error bodies are now `{"error": "..."}`, which
the frontend doesn't read (it only checks the status).

## 2. Dependency Inversion Principle

### What was wrong

The high-level service depended on two low-level, concrete classes:

```java
private final WatchlistRepository watchlistRepository; // the HashMap class itself
private final PlayerService playerService;              // the hard-coded player list
```

Neither was an abstraction. `WatchlistRepository` *was* the in-memory
`ConcurrentHashMap` store, and `PlayerService` was the class holding the
hard-coded roster.

### Why it made changes risky

Both classes are temporary stand-ins until the database module later in the
course. Because `WatchlistService` named them directly, replacing either one
would mean editing the watchlist's business logic even though no watchlist
rule had changed. The service also pulled the entire player list
(`getAllPlayers()`) just to answer "does this one player exist?" — it depended
on far more of `PlayerService` than it needed.

### The fix

`WatchlistRepository` is now an interface describing what the watchlist needs
from storage. The original HashMap code moved, unchanged, into
`InMemoryWatchlistRepository`:

```java
public interface WatchlistRepository {
    WatchlistEntry save(WatchlistEntry entry);
    List<WatchlistEntry> findAll();
    Optional<WatchlistEntry> findById(Long id);
    boolean deleteById(Long id);
}
```

Players are reached through a one-method interface that `PlayerService`
implements:

```java
public interface PlayerLookup {
    boolean existsById(Long playerId);
}
```

When the database arrives, a new class implements `WatchlistRepository` and
Spring injects it; `WatchlistService` doesn't change. (`PlayerLookup` was an
interim step. DEV-78 replaced it with `PlayerRepository` — see
[Follow-up](#follow-up-dev-78-repository-layer).)

## 3. Open/Closed Principle

### What was wrong

Every rule for "can this player be added?" was written inline:

```java
public WatchlistEntry createEntry(WatchlistEntry request) {
    boolean playerExists = playerService.getAllPlayers().stream()
        .anyMatch(player -> player.getId().equals(request.getPlayerId()));
    if (!playerExists) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found");
    }
    // ...save
}
```

### Why it made changes risky

There was one rule, but the next ones are predictable: no duplicate entries
(the backend currently allows them; only the disabled frontend button stops
it), a maximum watchlist size, a note length limit. Each would mean reopening
`createEntry` and editing it, risking the rules that already worked.

### The fix

Each rule is its own class implementing `WatchlistEntryValidator`:

```java
@Component
public class PlayerExistsValidator implements WatchlistEntryValidator {
    // ...
    @Override
    public void validate(WatchlistEntry request) {
        if (!playerLookup.existsById(request.getPlayerId())) {
            throw new PlayerNotFoundException(request.getPlayerId());
        }
    }
}
```

Spring injects every validator into `WatchlistService`, and `createEntry` runs
them all:

```java
validators.forEach(validator -> validator.validate(request));
```

A duplicate check becomes a new `NoDuplicateEntryValidator` class marked
`@Component`; `createEntry` never changes.

## Verification

All 28 backend tests pass. New and updated tests:

- `WatchlistServiceTest` — updated to inject a mocked validator list instead
  of mocking `PlayerService`; asserts domain exceptions, not status codes.
- `PlayerExistsValidatorTest` — known player passes, unknown player throws
  `PlayerNotFoundException`.
- `WatchlistControllerErrorTest` — end-to-end through the controller and
  `ApiExceptionHandler`: unknown player, missing update and missing delete all
  still return 404; a known player returns 201.

## Not changed here

- `docs/class-diagram.drawio` is a domain model (`Player`, `Team`,
  `WatchlistEntry`, …) with no services or repositories, so these changes
  don't affect it.
- `PlayerService` still held the hard-coded player data. DEV-78 moved it into
  a repository; see below.

## Follow-up: DEV-78 repository layer

DEV-79 left one SRP problem in place: `PlayerService` and `TeamService` held
their hard-coded data as well as their service logic. DEV-78 moved the data
into the repository layer behind a generic contract, split by what a
repository can do:

```java
public interface ReadOnlyRepository<T, ID> {
    List<T> findAll();
    Optional<T> findById(ID id);
    boolean existsById(ID id);
}

public interface ReadWriteRepository<T, ID> extends ReadOnlyRepository<T, ID> {
    T save(T entity);
    void deleteById(ID id);
}

public interface PlayerRepository    extends ReadOnlyRepository<Player, Long> {}
public interface TeamRepository      extends ReadOnlyRepository<Team, String> {}
public interface WatchlistRepository extends ReadWriteRepository<WatchlistEntry, Long> {}
```

| Interface | Today | Planned |
|---|---|---|
| `PlayerRepository` | `InMemoryPlayerRepository` | External-API implementation |
| `TeamRepository` | `InMemoryTeamRepository` | External-API implementation |
| `WatchlistRepository` | `InMemoryWatchlistRepository` | JPA (database module) |

Why split by capability rather than use one `Repository` with every method:
players and teams will come from an external API and are read-only. A single
interface with `save` and `deleteById` would force those repositories to
implement writes they can only refuse by throwing — an LSP violation (callers
can't trust the interface) and an ISP violation (implementers are forced to
answer for methods they can't support). Where the data comes from (API or
database) is not the reason for the split; hiding that is exactly what the
interface is for.

Other changes:

- Signatures match Spring Data, so a JPA repository can implement
  `WatchlistRepository` directly. `deleteById` now returns `void` and is a
  no-op for a missing id; `WatchlistService.deleteEntry` checks `existsById`
  first to keep answering 404.
- `PlayerExistsValidator` depends on `PlayerRepository`; `PlayerLookup` was
  deleted.
- The in-memory player and team repositories look up by id or code in a map,
  instead of scanning the whole list, and `findAll` keeps the original order.
  `GET /players` and `GET /teams` were verified byte-for-byte identical before
  and after the change.
- The data-integrity tests moved with the data, from `PlayerServiceTest` and
  `TeamServiceTest` into `InMemoryPlayerRepositoryTest` and
  `InMemoryTeamRepositoryTest`. The service tests now mock their repository.
  All 38 backend tests pass.
