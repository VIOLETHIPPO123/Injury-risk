package com.sidelined.backend.validation;

import com.sidelined.backend.exception.PlayerNotFoundException;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.repository.PlayerRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlayerExistsValidatorTest {

    // The signed-in user whose roster is being checked.
    private static final String USER = "user-a";

    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final PlayerExistsValidator validator = new PlayerExistsValidator(playerRepository);

    @Test
    void validate_knownPlayer_passes() {
        when(playerRepository.existsById(1L)).thenReturn(true);
        assertDoesNotThrow(() -> validator.validate(USER, new RosterEntry(null, 1L, null, "")));
    }

    @Test
    void validate_unknownPlayer_throwsPlayerNotFound() {
        when(playerRepository.existsById(9999L)).thenReturn(false);
        assertThrows(PlayerNotFoundException.class,
            () -> validator.validate(USER, new RosterEntry(null, 9999L, null, "")));
    }
}
