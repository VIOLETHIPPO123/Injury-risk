package com.sidelined.backend.validation;

import com.sidelined.backend.exception.RosterTooBigForFormatException;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.model.RosterFormat;
import com.sidelined.backend.repository.RosterEntryRepository;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static com.sidelined.backend.model.StandardRosterFormats.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RosterFitsFormatValidatorTest {

    // The roster the mocked repository returns. Tests add to it.
    private final List<RosterEntry> roster = new ArrayList<>();
    private final RosterEntryRepository rosterEntryRepository = mock(RosterEntryRepository.class);
    private final RosterFitsFormatValidator validator = new RosterFitsFormatValidator(rosterEntryRepository);

    // Yahoo Standard holds 15.
    private final RosterFormat yahoo = byId(YAHOO_STANDARD).orElseThrow();

    RosterFitsFormatValidatorTest() {
        when(rosterEntryRepository.findAll()).thenReturn(roster);
    }

    private void roster(int players, String slot) {
        for (int i = 0; i < players; i++) {
            long id = 100L + roster.size();
            roster.add(new RosterEntry(id, id, slot, ""));
        }
    }

    @Test
    void validate_rosterThatFits_passes() {
        roster(15, RosterEntry.BENCH);

        assertDoesNotThrow(() -> validator.validate(yahoo));
    }

    @Test
    void validate_rosterBiggerThanTheFormat_throws() {
        roster(16, RosterEntry.BENCH);

        assertThrows(RosterTooBigForFormatException.class, () -> validator.validate(yahoo));
    }

    @Test
    void validate_playersOnIr_countBecauseTheyMoveToTheBench() {
        roster(15, RosterEntry.BENCH);
        roster(1, RosterEntry.IR);

        assertThrows(RosterTooBigForFormatException.class, () -> validator.validate(yahoo));
    }
}
