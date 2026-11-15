package com.sidelined.backend.repository;

import com.sidelined.backend.model.RosterSettings;
import org.springframework.data.jpa.repository.JpaRepository;

// The user_roster_settings table: at most one row per user, added the first time they choose a
// format. Spring Data writes findByProfileId (it follows RosterSettings.profile to its id).
public interface JpaRosterSettingsRepository extends JpaRepository<RosterSettings, Long>, RosterSettingsRepository {

    // Both parents declare a save; naming it once here stops calls to it being ambiguous.
    @Override
    RosterSettings save(RosterSettings settings);
}
