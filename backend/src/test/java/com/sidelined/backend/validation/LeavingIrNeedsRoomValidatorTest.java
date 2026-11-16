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

class LeavingIrNeedsRoomValidatorTest {

    // The signed-in user whose roster is being checked.
    private static final String USER = "user-a";

    // The roster the mocked repository returns. Tests add to it.
    private final List<RosterEntry> roster = new ArrayList<>();
    private final RosterEntryRepository rosterEntryRepository = mock(RosterEntryRepository.class);
    private final CurrentRosterFormat currentFormat = mock(CurrentRosterFormat.class);
    private final LeavingIrNeedsRoomValidator validator =
        new LeavingIrNeedsRoomValidator(rosterEntryRepository, currentFormat);

    private final RosterEntry onIr = new RosterEntry(1L, 28L, RosterEntry.IR, "");

    LeavingIrNeedsRoomValidatorTest() {
        when(rosterEntryRepository.findByProfileId(USER)).thenReturn(roster);
        // ESPN Standard: 16 players plus 1 IR.
        when(currentFormat.get(USER)).thenReturn(byId(ESPN_STANDARD).orElseThrow());
        roster.add(onIr);
    }

    private void fillBench(int players) {
        for (long i = 0; i < players; i++) {
            roster.add(new RosterEntry(100L + i, 100L + i, RosterEntry.BENCH, ""));
        }
    }

    @Test
    void validate_offIr_withRoomOnTheRoster_passes() {
        fillBench(15);

        assertDoesNotThrow(() -> validator.validate(USER, onIr, RosterEntry.BENCH));
    }

    @Test
    void validate_offIr_withAFullRoster_throws() {
        fillBench(16);

        assertThrows(RosterFullException.class, () -> validator.validate(USER, onIr, RosterEntry.BENCH));
        assertThrows(RosterFullException.class, () -> validator.validate(USER, onIr, "TE"));
    }

    @Test
    void validate_playerNotOnIr_isntChecked() {
        fillBench(16);
        RosterEntry benched = roster.get(1);

        assertDoesNotThrow(() -> validator.validate(USER, benched, "WR"));
        verifyNoInteractions(currentFormat);
    }
}
