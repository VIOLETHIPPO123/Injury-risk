package com.sidelined.backend.validation;

import com.sidelined.backend.exception.PlayerNotFoundException;
import com.sidelined.backend.model.WatchlistEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerExistsValidatorTest {

    private final PlayerExistsValidator validator = new PlayerExistsValidator(id -> id == 1L);

    @Test
    void validate_knownPlayer_passes() {
        assertDoesNotThrow(() -> validator.validate(new WatchlistEntry(null, 1L, "")));
    }

    @Test
    void validate_unknownPlayer_throwsPlayerNotFound() {
        assertThrows(PlayerNotFoundException.class,
            () -> validator.validate(new WatchlistEntry(null, 9999L, "")));
    }
}
