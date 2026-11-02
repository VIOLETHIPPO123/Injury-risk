package com.sidelined.backend.validation;

import com.sidelined.backend.exception.RosterTooBigForFormatException;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.model.RosterFormat;
import com.sidelined.backend.repository.InMemoryRosterEntryRepository;
import com.sidelined.backend.repository.InMemoryRosterFormatRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RosterFitsFormatValidatorTest {

    private final InMemoryRosterEntryRepository rosterEntryRepository = new InMemoryRosterEntryRepository();
    private final RosterFitsFormatValidator validator = new RosterFitsFormatValidator(rosterEntryRepository);

    // Yahoo Standard holds 15.
    private final RosterFormat yahoo = new InMemoryRosterFormatRepository()
        .findById(InMemoryRosterFormatRepository.YAHOO_STANDARD).orElseThrow();

    private void roster(int players, String slot) {
        for (int i = 0; i < players; i++) {
            rosterEntryRepository.save(new RosterEntry(null, 100L + rosterEntryRepository.findAll().size(), slot, ""));
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
