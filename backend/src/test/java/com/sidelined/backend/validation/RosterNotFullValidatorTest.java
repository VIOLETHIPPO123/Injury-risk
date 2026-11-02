package com.sidelined.backend.validation;

import com.sidelined.backend.exception.RosterFullException;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.model.RosterSettings;
import com.sidelined.backend.repository.InMemoryRosterEntryRepository;
import com.sidelined.backend.repository.InMemoryRosterFormatRepository;
import com.sidelined.backend.repository.InMemoryRosterSettingsRepository;
import com.sidelined.backend.service.CurrentRosterFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RosterNotFullValidatorTest {

    private final InMemoryRosterEntryRepository rosterEntryRepository = new InMemoryRosterEntryRepository();
    private final InMemoryRosterSettingsRepository settingsRepository = new InMemoryRosterSettingsRepository();
    private final RosterNotFullValidator validator = new RosterNotFullValidator(rosterEntryRepository,
        new CurrentRosterFormat(new InMemoryRosterFormatRepository(), settingsRepository));

    private static final RosterEntry REQUEST = new RosterEntry(null, 999L, null, "");

    private void fill(int players, String slot) {
        for (int i = 0; i < players; i++) {
            rosterEntryRepository.save(new RosterEntry(null, 100L + i + rosterEntryRepository.findAll().size(), slot, ""));
        }
    }

    @Test
    void validate_belowTheMaximum_passes() {
        fill(15, RosterEntry.BENCH); // ESPN Standard holds 16

        assertDoesNotThrow(() -> validator.validate(REQUEST));
    }

    @Test
    void validate_atTheMaximum_throws() {
        fill(16, RosterEntry.BENCH);

        assertThrows(RosterFullException.class, () -> validator.validate(REQUEST));
    }

    @Test
    void validate_playersOnIr_dontCount() {
        fill(15, RosterEntry.BENCH);
        fill(1, RosterEntry.IR);

        assertDoesNotThrow(() -> validator.validate(REQUEST));
    }

    @Test
    void validate_usesTheLeaguesFormat() {
        settingsRepository.save(new RosterSettings(InMemoryRosterFormatRepository.YAHOO_STANDARD));
        fill(15, RosterEntry.BENCH); // Yahoo Standard holds 15

        assertThrows(RosterFullException.class, () -> validator.validate(REQUEST));
    }
}
