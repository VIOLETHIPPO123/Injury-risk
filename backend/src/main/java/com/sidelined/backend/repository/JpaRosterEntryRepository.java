package com.sidelined.backend.repository;

import com.sidelined.backend.model.RosterEntry;
import org.springframework.data.jpa.repository.JpaRepository;

// The roster_entries table. Spring Data writes the implementation; the services only see
// RosterEntryRepository.
public interface JpaRosterEntryRepository extends JpaRepository<RosterEntry, Long>, RosterEntryRepository {

    // Both parents declare a save; naming it once here stops calls to it being ambiguous.
    @Override
    RosterEntry save(RosterEntry entry);
}
