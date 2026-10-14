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
public class InMemoryWatchlistRepository implements WatchlistRepository {

    private final Map<Long, WatchlistEntry> entries = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    @Override
    public WatchlistEntry save(WatchlistEntry entry) {
        if (entry.getId() == null) {
            entry.setId(nextId.getAndIncrement());
        }
        entries.put(entry.getId(), entry);
        return entry;
    }

    @Override
    public List<WatchlistEntry> findAll() {
        return new ArrayList<>(entries.values());
    }

    @Override
    public Optional<WatchlistEntry> findById(Long id) {
        return Optional.ofNullable(entries.get(id));
    }

    @Override
    public boolean existsById(Long id) {
        return entries.containsKey(id);
    }

    // A missing id is a no-op, as in Spring Data. The service checks existsById first to answer 404.
    @Override
    public void deleteById(Long id) {
        entries.remove(id);
    }
}
