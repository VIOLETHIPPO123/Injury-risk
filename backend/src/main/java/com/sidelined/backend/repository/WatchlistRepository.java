package com.sidelined.backend.repository;

import com.sidelined.backend.model.WatchlistEntry;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

// In-memory store until a database is wired in. Data resets when the backend restarts.
@Repository
public class WatchlistRepository {

    private final Map<Long, WatchlistEntry> entries = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public WatchlistEntry save(WatchlistEntry entry) {
        if (entry.getId() == null) {
            entry.setId(nextId.getAndIncrement());
        }
        entries.put(entry.getId(), entry);
        return entry;
    }

    public List<WatchlistEntry> findAll() {
        return new ArrayList<>(entries.values());
    }

    public Optional<WatchlistEntry> findById(Long id) {
        return Optional.ofNullable(entries.get(id));
    }

    // Returns false when the id was not on the watchlist, so the service can answer 404.
    public boolean deleteById(Long id) {
        return entries.remove(id) != null;
    }
}
