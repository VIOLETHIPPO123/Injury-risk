package com.sidelined.backend.repository;

import com.sidelined.backend.model.RosterFormat;
import org.springframework.data.jpa.repository.JpaRepository;

// The roster_formats reference table. Only RosterDataLoader writes to it, at startup;
// everything else reads it through RosterFormatRepository.
public interface JpaRosterFormatRepository extends JpaRepository<RosterFormat, String>, RosterFormatRepository {
}
