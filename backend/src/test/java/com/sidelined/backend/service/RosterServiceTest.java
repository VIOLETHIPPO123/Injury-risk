package com.sidelined.backend.service;

import com.sidelined.backend.exception.UnknownRosterFormatException;
import com.sidelined.backend.model.RosterSettings;
import com.sidelined.backend.repository.InMemoryRosterFormatRepository;
import com.sidelined.backend.repository.InMemoryRosterSettingsRepository;
import org.junit.jupiter.api.Test;

import static com.sidelined.backend.repository.InMemoryRosterFormatRepository.*;
import static org.junit.jupiter.api.Assertions.*;

class RosterServiceTest {

    private final RosterService rosterService = new RosterService(
        new InMemoryRosterFormatRepository(), new InMemoryRosterSettingsRepository());

    @Test
    void settings_defaultToEspnStandard() {
        assertEquals(ESPN_STANDARD, rosterService.getSettings().formatId());
    }

    @Test
    void updateSettings_knownFormat_isSaved() {
        rosterService.updateSettings(new RosterSettings(SLEEPER_DEFAULT));

        assertEquals(SLEEPER_DEFAULT, rosterService.getSettings().formatId());
    }

    @Test
    void updateSettings_unknownFormat_throwsAndKeepsTheOldOne() {
        assertThrows(UnknownRosterFormatException.class,
            () -> rosterService.updateSettings(new RosterSettings("nope")));

        assertEquals(ESPN_STANDARD, rosterService.getSettings().formatId());
    }

    @Test
    void updateSettings_missingFormat_throws() {
        assertThrows(UnknownRosterFormatException.class,
            () -> rosterService.updateSettings(new RosterSettings(null)));
    }
}
