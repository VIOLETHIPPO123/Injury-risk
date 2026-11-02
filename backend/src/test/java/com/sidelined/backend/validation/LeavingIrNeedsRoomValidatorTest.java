package com.sidelined.backend.validation;

import com.sidelined.backend.exception.RosterFullException;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.repository.InMemoryRosterEntryRepository;
import com.sidelined.backend.repository.InMemoryRosterFormatRepository;
import com.sidelined.backend.repository.InMemoryRosterSettingsRepository;
import com.sidelined.backend.service.CurrentRosterFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LeavingIrNeedsRoomValidatorTest {

    private final InMemoryRosterEntryRepository rosterEntryRepository = new InMemoryRosterEntryRepository();
    // ESPN Standard: 16 players plus 1 IR.
    private final LeavingIrNeedsRoomValidator validator = new LeavingIrNeedsRoomValidator(rosterEntryRepository,
        new CurrentRosterFormat(new InMemoryRosterFormatRepository(), new InMemoryRosterSettingsRepository()));

    private final RosterEntry onIr = rosterEntryRepository.save(new RosterEntry(null, 28L, RosterEntry.IR, ""));

    private void fillBench(int players) {
        for (long i = 0; i < players; i++) {
            rosterEntryRepository.save(new RosterEntry(null, 100L + i, RosterEntry.BENCH, ""));
        }
    }

    @Test
    void validate_offIr_withRoomOnTheRoster_passes() {
        fillBench(15);

        assertDoesNotThrow(() -> validator.validate(onIr, RosterEntry.BENCH));
    }

    @Test
    void validate_offIr_withAFullRoster_throws() {
        fillBench(16);

        assertThrows(RosterFullException.class, () -> validator.validate(onIr, RosterEntry.BENCH));
        assertThrows(RosterFullException.class, () -> validator.validate(onIr, "TE"));
    }

    @Test
    void validate_playerNotOnIr_isntChecked() {
        fillBench(16);
        RosterEntry benched = rosterEntryRepository.findAll().stream()
            .filter(e -> RosterEntry.BENCH.equals(e.getSlot())).findFirst().orElseThrow();

        assertDoesNotThrow(() -> validator.validate(benched, "WR"));
    }
}
