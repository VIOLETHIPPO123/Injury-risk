package com.sidelined.backend.repository;

import com.sidelined.backend.model.RosterSettings;

// The league has one settings record, so there is no id: read it or replace it.
// Today: InMemoryRosterSettingsRepository.
public interface RosterSettingsRepository {
    RosterSettings get();
    RosterSettings save(RosterSettings settings);
}
