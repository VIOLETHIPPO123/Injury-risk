package com.sidelined.backend.validation;

import com.sidelined.backend.exception.NotFantasyEligibleException;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.repository.InMemoryPlayerRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FantasyEligibleValidatorTest {

    private final FantasyEligibleValidator validator =
        new FantasyEligibleValidator(new InMemoryPlayerRepository());

    private static RosterEntry add(long playerId) {
        return new RosterEntry(null, playerId, null, "");
    }

    @Test
    void validate_fantasyPositions_pass() {
        // QB, RB, WR, TE, FB, PK
        for (long id : new long[] {1L, 2L, 3L, 6L, 84L, 791L}) {
            assertDoesNotThrow(() -> validator.validate(add(id)), "Player " + id);
        }
    }

    @Test
    void validate_ineligiblePositions_throwWithTheMessage() {
        // LT, DE, CB, KR, PR, H, P, LS
        for (long id : new long[] {7L, 391L, 492L, 813L, 770L, 769L, 792L, 892L}) {
            NotFantasyEligibleException ex = assertThrows(NotFantasyEligibleException.class,
                () -> validator.validate(add(id)), "Player " + id);
            assertEquals("Not a fantasy-eligible position.", ex.getMessage());
        }
    }

    @Test
    void validate_unknownPlayer_isLeftToPlayerExistsValidator() {
        assertDoesNotThrow(() -> validator.validate(add(9999L)));
    }
}
