package com.sidelined.backend.repository;

import com.sidelined.backend.model.RosterSettings;
import org.springframework.stereotype.Repository;

// In memory until a database is wired in. Starts on ESPN Standard and resets to it on restart.
@Repository
public class InMemoryRosterSettingsRepository implements RosterSettingsRepository {

    private volatile RosterSettings settings =
        new RosterSettings(InMemoryRosterFormatRepository.ESPN_STANDARD);

    @Override
    public RosterSettings get() {
        return settings;
    }

    @Override
    public RosterSettings save(RosterSettings settings) {
        this.settings = settings;
        return settings;
    }
}
