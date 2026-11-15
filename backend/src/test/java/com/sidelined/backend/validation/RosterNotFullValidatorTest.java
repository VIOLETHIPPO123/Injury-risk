package com.sidelined.backend.validation;

import com.sidelined.backend.exception.RosterFullException;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.repository.RosterEntryRepository;
import com.sidelined.backend.service.CurrentRosterFormat;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static com.sidelined.backend.model.StandardRosterFormats.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RosterNotFullValidatorTest {

    // The roster the mocked repository returns. Tests add to it.
    private final List<RosterEntry> roster = new ArrayList<>();
    private final RosterEntryRepository rosterEntryRepository = mock(RosterEntryRepository.class);
    private final CurrentRosterFormat currentFormat = mock(CurrentRosterFormat.class);
    private final RosterNotFullValidator validator = new RosterNotFullValidator(rosterEntryRepository, currentFormat);

    private static final RosterEntry REQUEST = new RosterEntry(null, 999L, null, "");

    RosterNotFullValidatorTest() {
        when(rosterEntryRepository.findAll()).thenReturn(roster);
        when(currentFormat.get()).thenReturn(byId(ESPN_STANDARD).orElseThrow());
    }

    private void fill(int players, String slot) {
        for (int i = 0; i < players; i++) {
            long id = 100L + roster.size();
            roster.add(new RosterEntry(id, id, slot, ""));
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
        when(currentFormat.get()).thenReturn(byId(YAHOO_STANDARD).orElseThrow());
        fill(15, RosterEntry.BENCH); // Yahoo Standard holds 15

        assertThrows(RosterFullException.class, () -> validator.validate(REQUEST));
    }
}
