package com.sidelined.backend.repository;

import com.sidelined.backend.model.RosterEntry;
import java.util.List;
import java.util.Optional;

// Read-write: the roster is the app's own data. Today: JpaRosterEntryRepository.
// Every roster belongs to one user, so the services read through the ByProfileId methods;
// findAll and findById see everyone's entries and are only for maintenance and tests.
public interface RosterEntryRepository extends ReadWriteRepository<RosterEntry, Long> {
    // One user's roster, in no particular order.
    List<RosterEntry> findByProfileId(String profileId);

    // The entry, but only if it is on this user's roster: another user's entry is "not there".
    Optional<RosterEntry> findByIdAndProfileId(Long id, String profileId);
}
