package com.sidelined.backend.config;

import com.sidelined.backend.exception.NflverseUnavailableException;
import com.sidelined.backend.repository.JpaPlayerRepository;
import com.sidelined.backend.service.PlayerSyncJob;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlayerDataLoaderTest {

    private final JpaPlayerRepository playerRepository = mock(JpaPlayerRepository.class);
    private final PlayerSyncJob playerSyncJob = mock(PlayerSyncJob.class);
    private final PlayerDataLoader loader = new PlayerDataLoader(playerRepository, playerSyncJob);

    @Test
    void emptyTable_runsTheSync() {
        when(playerRepository.count()).thenReturn(0L);

        loader.run(null);

        verify(playerSyncJob).sync();
    }

    @Test
    void tableWithPlayers_isLeftToTheWeeklyJob() {
        when(playerRepository.count()).thenReturn(2053L);

        loader.run(null);

        verify(playerSyncJob, never()).sync();
    }

    @Test
    void nflverseDown_doesntStopTheAppStarting() {
        when(playerRepository.count()).thenReturn(0L);
        when(playerSyncJob.sync()).thenThrow(new NflverseUnavailableException("down"));

        assertDoesNotThrow(() -> loader.run(null));
    }
}
