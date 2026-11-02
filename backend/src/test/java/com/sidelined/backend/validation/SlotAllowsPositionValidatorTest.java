package com.sidelined.backend.validation;

import com.sidelined.backend.exception.SlotNotAllowedException;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.repository.InMemoryPlayerRepository;
import com.sidelined.backend.repository.InMemoryRosterFormatRepository;
import com.sidelined.backend.repository.InMemoryRosterSettingsRepository;
import com.sidelined.backend.service.CurrentRosterFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SlotAllowsPositionValidatorTest {

    // ESPN Standard, the default format.
    private final SlotAllowsPositionValidator validator = new SlotAllowsPositionValidator(
        new InMemoryPlayerRepository(),
        new CurrentRosterFormat(new InMemoryRosterFormatRepository(), new InMemoryRosterSettingsRepository()));

    private static RosterEntry onBench(long playerId) {
        return new RosterEntry(1L, playerId, RosterEntry.BENCH, "");
    }

    @Test
    void validate_positionListedInTheSlot_passes() {
        assertDoesNotThrow(() -> validator.validate(onBench(1L), "QB"));   // QB
        assertDoesNotThrow(() -> validator.validate(onBench(6L), "FLEX")); // TE
        assertDoesNotThrow(() -> validator.validate(onBench(84L), "RB"));  // FB counts as RB
        assertDoesNotThrow(() -> validator.validate(onBench(791L), "K"));  // PK counts as K
    }

    @Test
    void validate_positionNotListedInTheSlot_throws() {
        assertThrows(SlotNotAllowedException.class, () -> validator.validate(onBench(1L), "RB"));
        assertThrows(SlotNotAllowedException.class, () -> validator.validate(onBench(1L), "FLEX"));
        assertThrows(SlotNotAllowedException.class, () -> validator.validate(onBench(3L), "DEF"));
    }

    @Test
    void validate_benchAndIr_takeAnyone() {
        assertDoesNotThrow(() -> validator.validate(onBench(1L), RosterEntry.BENCH));
        assertDoesNotThrow(() -> validator.validate(onBench(1L), RosterEntry.IR));
    }

    @Test
    void validate_slotTheFormatDoesntHave_throws() {
        assertThrows(SlotNotAllowedException.class, () -> validator.validate(onBench(1L), "SUPERFLEX"));
        assertThrows(SlotNotAllowedException.class, () -> validator.validate(onBench(1L), null));
    }
}
