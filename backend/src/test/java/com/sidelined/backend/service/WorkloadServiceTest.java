package com.sidelined.backend.service;

import com.sidelined.backend.exception.PlayerNotFoundException;
import com.sidelined.backend.model.WorkloadEntry;
import com.sidelined.backend.repository.PlayerRepository;
import com.sidelined.backend.repository.WorkloadRepository;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkloadServiceTest {

    private final WorkloadRepository workloadRepository = mock(WorkloadRepository.class);
    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final WorkloadService workloadService =
        new WorkloadService(workloadRepository, playerRepository);

    @Test
    void getWorkload_knownPlayer_returnsTheRepositoryHistory() {
        // Arrange
        List<WorkloadEntry> history = List.of(new WorkloadEntry(1L, 1, 50), new WorkloadEntry(1L, 3, 55));
        when(playerRepository.existsById(1L)).thenReturn(true);
        when(workloadRepository.findByPlayerId(1L)).thenReturn(history);

        // Act
        List<WorkloadEntry> result = workloadService.getWorkload(1L);

        // Assert
        assertEquals(history, result);
    }

    @Test
    void getWorkload_unknownPlayer_throwsPlayerNotFound() {
        // Arrange
        when(playerRepository.existsById(9999L)).thenReturn(false);

        // Act + Assert
        assertThrows(PlayerNotFoundException.class, () -> workloadService.getWorkload(9999L));
        verifyNoInteractions(workloadRepository);
    }
}
