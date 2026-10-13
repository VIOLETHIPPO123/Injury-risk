package com.sidelined.backend.validation;

import com.sidelined.backend.exception.PlayerNotFoundException;
import com.sidelined.backend.model.WatchlistEntry;
import com.sidelined.backend.repository.PlayerRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlayerExistsValidatorTest {

    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final PlayerExistsValidator validator = new PlayerExistsValidator(playerRepository);

    @Test
    void validate_knownPlayer_passes() {
        when(playerRepository.existsById(1L)).thenReturn(true);
        assertDoesNotThrow(() -> validator.validate(new WatchlistEntry(null, 1L, "")));
    }

    @Test
    void validate_unknownPlayer_throwsPlayerNotFound() {
        when(playerRepository.existsById(9999L)).thenReturn(false);
        assertThrows(PlayerNotFoundException.class,
            () -> validator.validate(new WatchlistEntry(null, 9999L, "")));
    }
}
