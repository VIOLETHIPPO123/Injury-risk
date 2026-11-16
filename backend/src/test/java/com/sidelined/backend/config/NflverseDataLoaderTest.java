package com.sidelined.backend.config;

import com.sidelined.backend.exception.NflverseUnavailableException;
import com.sidelined.backend.repository.JpaPlayerRepository;
import com.sidelined.backend.repository.JpaWorkloadRepository;
import com.sidelined.backend.service.PlayerSyncJob;
import com.sidelined.backend.service.WorkloadSyncJob;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NflverseDataLoaderTest {

    private final JpaPlayerRepository playerRepository = mock(JpaPlayerRepository.class);
    private final JpaWorkloadRepository workloadRepository = mock(JpaWorkloadRepository.class);
    private final PlayerSyncJob playerSyncJob = mock(PlayerSyncJob.class);
    private final WorkloadSyncJob workloadSyncJob = mock(WorkloadSyncJob.class);
    private final NflverseDataLoader loader =
        new NflverseDataLoader(playerRepository, workloadRepository, playerSyncJob, workloadSyncJob);

    @Test
    void emptyTables_runBothSyncs_playersFirst() {
        when(playerRepository.count()).thenReturn(0L);
        when(workloadRepository.count()).thenReturn(0L);

        loader.run(null);

        InOrder order = inOrder(playerSyncJob, workloadSyncJob);
        order.verify(playerSyncJob).sync();
        order.verify(workloadSyncJob).sync();
    }

    @Test
    void tablesWithRows_areLeftToTheWeeklyJob() {
        when(playerRepository.count()).thenReturn(2052L);
        when(workloadRepository.count()).thenReturn(12000L);

        loader.run(null);

        verify(playerSyncJob, never()).sync();
        verify(workloadSyncJob, never()).sync();
    }

    @Test
    void playersStoredButNoSnapCounts_runsOnlyTheWorkloadSync() {
        // The first start after upgrading from before the workload table existed.
        when(playerRepository.count()).thenReturn(2052L);
        when(workloadRepository.count()).thenReturn(0L);

        loader.run(null);

        verify(playerSyncJob, never()).sync();
        verify(workloadSyncJob).sync();
    }

    @Test
    void nflverseDown_doesntStopTheAppStarting() {
        when(playerRepository.count()).thenReturn(0L);
        when(workloadRepository.count()).thenReturn(0L);
        when(playerSyncJob.sync()).thenThrow(new NflverseUnavailableException("down"));
        when(workloadSyncJob.sync()).thenThrow(new NflverseUnavailableException("down"));

        assertDoesNotThrow(() -> loader.run(null));
    }
}
