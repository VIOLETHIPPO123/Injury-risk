package com.sidelined.backend.validation;

import com.sidelined.backend.exception.PlayerAlreadyOnRosterException;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.repository.InMemoryRosterEntryRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotAlreadyOnRosterValidatorTest {

    private final InMemoryRosterEntryRepository rosterEntryRepository = new InMemoryRosterEntryRepository();
    private final NotAlreadyOnRosterValidator validator = new NotAlreadyOnRosterValidator(rosterEntryRepository);

    @Test
    void validate_playerNotOnRoster_passes() {
        rosterEntryRepository.save(new RosterEntry(null, 2L, RosterEntry.BENCH, ""));

        assertDoesNotThrow(() -> validator.validate(new RosterEntry(null, 1L, null, "")));
    }

    @Test
    void validate_playerAlreadyOnRoster_throws() {
        rosterEntryRepository.save(new RosterEntry(null, 1L, RosterEntry.BENCH, ""));

        assertThrows(PlayerAlreadyOnRosterException.class,
            () -> validator.validate(new RosterEntry(null, 1L, null, "")));
    }
}
