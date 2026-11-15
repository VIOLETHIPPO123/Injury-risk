package com.sidelined.backend.validation;

import com.sidelined.backend.exception.SlotFullException;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.repository.RosterEntryRepository;
import com.sidelined.backend.service.CurrentRosterFormat;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static com.sidelined.backend.model.StandardRosterFormats.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SlotHasRoomValidatorTest {

    // The roster the mocked repository returns. Tests add to it.
    private final List<RosterEntry> roster = new ArrayList<>();
    private final RosterEntryRepository rosterEntryRepository = mock(RosterEntryRepository.class);
    private final CurrentRosterFormat currentFormat = mock(CurrentRosterFormat.class);
    private final SlotHasRoomValidator validator = new SlotHasRoomValidator(rosterEntryRepository, currentFormat);

    SlotHasRoomValidatorTest() {
        when(rosterEntryRepository.findAll()).thenReturn(roster);
        when(currentFormat.get()).thenReturn(byId(ESPN_STANDARD).orElseThrow());
    }

    // Entry ids go up from 1 in the order players are added, as the database assigns them.
    private RosterEntry rostered(long playerId, String slot) {
        RosterEntry entry = new RosterEntry(roster.size() + 1L, playerId, slot, "");
        roster.add(entry);
        return entry;
    }

    private void leagueIsOn(String formatId) {
        when(currentFormat.get()).thenReturn(byId(formatId).orElseThrow());
    }

    @Test
    void validate_slotWithAnOpenSpot_passes() {
        rostered(2L, "RB");
        RosterEntry mover = rostered(13L, RosterEntry.BENCH);

        assertDoesNotThrow(() -> validator.validate(mover, "RB")); // RB holds 2
    }

    @Test
    void validate_filledSlot_throws() {
        rostered(1L, "QB");
        RosterEntry mover = rostered(12L, RosterEntry.BENCH);

        assertThrows(SlotFullException.class, () -> validator.validate(mover, "QB"));
    }

    @Test
    void validate_theSlotThePlayerIsAlreadyIn_passes() {
        RosterEntry mover = rostered(1L, "QB");

        assertDoesNotThrow(() -> validator.validate(mover, "QB"));
    }

    @Test
    void validate_irHoldsTheFormatsIrCount() {
        rostered(28L, RosterEntry.IR);
        RosterEntry mover = rostered(70L, RosterEntry.BENCH);

        // ESPN Standard has 1 IR spot, Yahoo Standard 2.
        assertThrows(SlotFullException.class, () -> validator.validate(mover, RosterEntry.IR));
        leagueIsOn(YAHOO_STANDARD);
        assertDoesNotThrow(() -> validator.validate(mover, RosterEntry.IR));
    }

    @Test
    void validate_formatWithNoIr_turnsEveryoneAway() {
        leagueIsOn(SLEEPER_DEFAULT);
        RosterEntry mover = rostered(28L, RosterEntry.BENCH);

        assertThrows(SlotFullException.class, () -> validator.validate(mover, RosterEntry.IR));
    }

    @Test
    void validate_bench_neverFills() {
        for (long id = 100; id < 116; id++) {
            rostered(id, RosterEntry.BENCH);
        }
        RosterEntry mover = rostered(1L, "QB");

        assertDoesNotThrow(() -> validator.validate(mover, RosterEntry.BENCH));
        verifyNoInteractions(currentFormat);
    }
}
