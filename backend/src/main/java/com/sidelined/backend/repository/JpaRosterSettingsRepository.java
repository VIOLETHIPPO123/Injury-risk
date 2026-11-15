package com.sidelined.backend.repository;

import com.sidelined.backend.model.RosterSettings;
import org.springframework.data.jpa.repository.JpaRepository;

// The roster_settings table: one row, id 1. RosterDataLoader adds it at startup. Saving
// always writes to that row, because every RosterSettings has id 1.
public interface JpaRosterSettingsRepository extends JpaRepository<RosterSettings, Long>, RosterSettingsRepository {

    // Both parents declare a save; naming it once here stops calls to it being ambiguous.
    @Override
    RosterSettings save(RosterSettings settings);

    // If the row has been deleted (say, in the Table Editor), the league is on the default format.
    @Override
    default RosterSettings get() {
        return findById(RosterSettings.ID).orElseGet(RosterSettings::defaults);
    }
}
