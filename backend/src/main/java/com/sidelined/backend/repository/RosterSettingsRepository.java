package com.sidelined.backend.repository;

import com.sidelined.backend.model.RosterSettings;
import java.util.Optional;

// Each user has at most one settings record, found by their profile (Supabase user) id.
// Today: JpaRosterSettingsRepository.
public interface RosterSettingsRepository {
    // The user's saved settings, if they've ever chosen a format.
    Optional<RosterSettings> findByProfileId(String profileId);

    // The user's settings, or the defaults (unsaved) if they've never chosen a format.
    default RosterSettings getFor(String profileId) {
        return findByProfileId(profileId).orElseGet(RosterSettings::defaults);
    }

    RosterSettings save(RosterSettings settings);
}
