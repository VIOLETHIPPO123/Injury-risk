package com.sidelined.backend.repository;

import com.sidelined.backend.model.WatchlistEntry;

// Read-write: the watchlist is the app's own data.
// Today: InMemoryWatchlistRepository. After the database module: a JPA-backed class.
public interface WatchlistRepository extends ReadWriteRepository<WatchlistEntry, Long> {
}
