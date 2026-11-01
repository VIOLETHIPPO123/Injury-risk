package com.sidelined.backend.service;

import com.sidelined.backend.exception.PlayerNotFoundException;
import com.sidelined.backend.model.Injury;
import com.sidelined.backend.repository.InjuryRepository;
import com.sidelined.backend.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InjuryServiceTest {

    private final InjuryRepository injuryRepository = mock(InjuryRepository.class);
    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final InjuryService injuryService = new InjuryService(injuryRepository, playerRepository);

    @Test
    void getInjuries_returnsNewestFirst_bySeasonThenWeek() {
        // Arrange
        Injury oldest = new Injury(2023, 5, "Hamstring", "Strain", 1, "Recovered");
        Injury lateIn2025 = new Injury(2025, 12, "Ankle", "Sprain", 2, "Recovered");
        Injury earlyIn2025 = new Injury(2025, 3, "Knee", "Sprain", 0, "Recovered");
        when(playerRepository.existsById(1L)).thenReturn(true);
        when(injuryRepository.findByPlayerId(1L)).thenReturn(List.of(oldest, earlyIn2025, lateIn2025));

        // Act
        List<Injury> result = injuryService.getInjuries(1L);

        // Assert
        assertEquals(List.of(lateIn2025, earlyIn2025, oldest), result);
    }

    @Test
    void getInjuries_knownPlayerWithNoInjuries_returnsEmptyList() {
        // Arrange
        when(playerRepository.existsById(1L)).thenReturn(true);
        when(injuryRepository.findByPlayerId(1L)).thenReturn(List.of());

        // Act + Assert
        assertTrue(injuryService.getInjuries(1L).isEmpty());
    }

    @Test
    void getInjuries_unknownPlayer_throwsPlayerNotFound() {
        // Arrange
        when(playerRepository.existsById(9999L)).thenReturn(false);

        // Act + Assert
        assertThrows(PlayerNotFoundException.class, () -> injuryService.getInjuries(9999L));
        verifyNoInteractions(injuryRepository);
    }
}
