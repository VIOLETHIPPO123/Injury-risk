package com.sidelined.backend.service;

import com.sidelined.backend.exception.RosterTooBigForFormatException;
import com.sidelined.backend.exception.UnknownRosterFormatException;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.model.RosterSettings;
import com.sidelined.backend.repository.InMemoryRosterEntryRepository;
import com.sidelined.backend.repository.InMemoryRosterFormatRepository;
import com.sidelined.backend.repository.InMemoryRosterSettingsRepository;
import com.sidelined.backend.validation.RosterFitsFormatValidator;
import org.junit.jupiter.api.Test;
import java.util.List;

import static com.sidelined.backend.repository.InMemoryRosterFormatRepository.*;
import static org.junit.jupiter.api.Assertions.*;

class RosterServiceTest {

    private final InMemoryRosterEntryRepository rosterEntryRepository = new InMemoryRosterEntryRepository();
    private final RosterService rosterService = new RosterService(
        new InMemoryRosterFormatRepository(), new InMemoryRosterSettingsRepository(),
        rosterEntryRepository, List.of(new RosterFitsFormatValidator(rosterEntryRepository)));

    private void roster(long playerId, String slot) {
        rosterEntryRepository.save(new RosterEntry(null, playerId, slot, ""));
    }

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

    @Test
    void updateSettings_movesEveryPlayerToTheBench() {
        roster(1L, "QB");
        roster(2L, "RB");
        roster(28L, RosterEntry.IR);

        rosterService.updateSettings(new RosterSettings(YAHOO_STANDARD));

        assertTrue(rosterEntryRepository.findAll().stream()
            .allMatch(e -> RosterEntry.BENCH.equals(e.getSlot())));
    }

    @Test
    void updateSettings_rosterTooBigForTheNewFormat_throwsAndChangesNothing() {
        roster(1L, "QB");
        for (long id = 100; id < 115; id++) {
            roster(id, RosterEntry.BENCH);
        } // 16 players; Yahoo Standard holds 15

        assertThrows(RosterTooBigForFormatException.class,
            () -> rosterService.updateSettings(new RosterSettings(YAHOO_STANDARD)));

        assertEquals(ESPN_STANDARD, rosterService.getSettings().formatId());
        assertEquals("QB", rosterEntryRepository.findById(1L).orElseThrow().getSlot());
    }
}
