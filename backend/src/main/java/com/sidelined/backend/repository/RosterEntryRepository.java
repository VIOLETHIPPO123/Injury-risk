package com.sidelined.backend.repository;

import com.sidelined.backend.model.RosterEntry;

// Read-write: the roster is the app's own data.
// Today: InMemoryRosterEntryRepository. After the database module: a JPA-backed class.
public interface RosterEntryRepository extends ReadWriteRepository<RosterEntry, Long> {
}
