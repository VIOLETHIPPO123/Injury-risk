package com.sidelined.backend.repository;

import com.sidelined.backend.model.RosterEntry;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

// In-memory store until a database is wired in. Data resets when the backend restarts.
@Repository
public class InMemoryRosterEntryRepository implements RosterEntryRepository {

    private final Map<Long, RosterEntry> entries = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    @Override
    public RosterEntry save(RosterEntry entry) {
        if (entry.getId() == null) {
            entry.setId(nextId.getAndIncrement());
        }
        entries.put(entry.getId(), entry);
        return entry;
    }

    @Override
    public List<RosterEntry> findAll() {
        return new ArrayList<>(entries.values());
    }

    @Override
    public Optional<RosterEntry> findById(Long id) {
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
