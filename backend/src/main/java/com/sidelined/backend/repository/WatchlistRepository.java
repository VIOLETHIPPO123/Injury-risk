package com.sidelined.backend.repository;

import com.sidelined.backend.model.WatchlistEntry;
import java.util.List;
import java.util.Optional;

// What the watchlist needs from storage, with no say in HOW it's stored.
// Today: InMemoryWatchlistRepository. After the database module: a JPA-backed class.
public interface WatchlistRepository {
    WatchlistEntry save(WatchlistEntry entry);
    List<WatchlistEntry> findAll();
    Optional<WatchlistEntry> findById(Long id);
    boolean deleteById(Long id);
}
