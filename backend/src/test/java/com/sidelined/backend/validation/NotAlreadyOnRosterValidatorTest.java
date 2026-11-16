package com.sidelined.backend.validation;

import com.sidelined.backend.exception.PlayerAlreadyOnRosterException;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.repository.RosterEntryRepository;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotAlreadyOnRosterValidatorTest {

    // The signed-in user whose roster is being checked.
    private static final String USER = "user-a";

    private final RosterEntryRepository rosterEntryRepository = mock(RosterEntryRepository.class);
    private final NotAlreadyOnRosterValidator validator = new NotAlreadyOnRosterValidator(rosterEntryRepository);

    @Test
    void validate_playerNotOnRoster_passes() {
        when(rosterEntryRepository.findByProfileId(USER)).thenReturn(List.of(new RosterEntry(1L, 2L, RosterEntry.BENCH, "")));

        assertDoesNotThrow(() -> validator.validate(USER, new RosterEntry(null, 1L, null, "")));
    }

    @Test
    void validate_playerAlreadyOnRoster_throws() {
        when(rosterEntryRepository.findByProfileId(USER)).thenReturn(List.of(new RosterEntry(1L, 1L, RosterEntry.BENCH, "")));

        assertThrows(PlayerAlreadyOnRosterException.class,
            () -> validator.validate(USER, new RosterEntry(null, 1L, null, "")));
    }
}
