package com.sidelined.backend.validation;

import com.sidelined.backend.exception.SlotFullException;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.model.RosterSettings;
import com.sidelined.backend.repository.InMemoryRosterEntryRepository;
import com.sidelined.backend.repository.InMemoryRosterFormatRepository;
import com.sidelined.backend.repository.InMemoryRosterSettingsRepository;
import com.sidelined.backend.service.CurrentRosterFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SlotHasRoomValidatorTest {

    private final InMemoryRosterEntryRepository rosterEntryRepository = new InMemoryRosterEntryRepository();
    private final InMemoryRosterSettingsRepository settingsRepository = new InMemoryRosterSettingsRepository();
    private final SlotHasRoomValidator validator = new SlotHasRoomValidator(rosterEntryRepository,
        new CurrentRosterFormat(new InMemoryRosterFormatRepository(), settingsRepository));

    private RosterEntry rostered(long playerId, String slot) {
        return rosterEntryRepository.save(new RosterEntry(null, playerId, slot, ""));
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
        settingsRepository.save(new RosterSettings(InMemoryRosterFormatRepository.YAHOO_STANDARD));
        assertDoesNotThrow(() -> validator.validate(mover, RosterEntry.IR));
    }

    @Test
    void validate_formatWithNoIr_turnsEveryoneAway() {
        settingsRepository.save(new RosterSettings(InMemoryRosterFormatRepository.SLEEPER_DEFAULT));
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
    }
}
