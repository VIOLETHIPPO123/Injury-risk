package com.sidelined.backend.repository;

import com.sidelined.backend.model.RosterFormat;

// Read-only and keyed by format id (e.g. "espn-standard"). Today: InMemoryRosterFormatRepository.
public interface RosterFormatRepository extends ReadOnlyRepository<RosterFormat, String> {
}
