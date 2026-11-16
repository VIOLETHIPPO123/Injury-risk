package com.sidelined.backend.config;

import com.sidelined.backend.exception.NflverseUnavailableException;
import com.sidelined.backend.service.PlayerSyncJob;
import com.sidelined.backend.service.WorkloadSyncJob;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WeeklyNflverseSyncTest {

    private final PlayerSyncJob playerSyncJob = mock(PlayerSyncJob.class);
    private final WorkloadSyncJob workloadSyncJob = mock(WorkloadSyncJob.class);
    private final WeeklyNflverseSync weekly = new WeeklyNflverseSync(playerSyncJob, workloadSyncJob);

    @Test
    void run_syncsPlayersFirst_thenWorkloads() {
        weekly.run();

        InOrder order = inOrder(playerSyncJob, workloadSyncJob);
        order.verify(playerSyncJob).sync();
        order.verify(workloadSyncJob).sync();
    }

    @Test
    void playerSyncFails_theWorkloadSyncStillRuns_andNothingEscapesTheScheduler() {
        when(playerSyncJob.sync()).thenThrow(new NflverseUnavailableException("down"));

        assertDoesNotThrow(weekly::run);

        verify(workloadSyncJob).sync();
    }

    @Test
    void workloadSyncFails_nothingEscapesTheScheduler() {
        when(workloadSyncJob.sync()).thenThrow(new NflverseUnavailableException("down"));

        assertDoesNotThrow(weekly::run);
    }

    @Test
    void schedule_isEveryTuesdayAt2pmCentral() throws NoSuchMethodException {
        // Arrange
        Scheduled scheduled = WeeklyNflverseSync.class.getMethod("run").getAnnotation(Scheduled.class);
        ZoneId central = ZoneId.of("America/Chicago");
        // A Monday night, as the Monday game ends.
        ZonedDateTime mondayNight = ZonedDateTime.of(2026, 11, 16, 23, 0, 0, 0, central);

        // Act
        ZonedDateTime next = CronExpression.parse(scheduled.cron()).next(mondayNight);
        ZonedDateTime oneAfter = CronExpression.parse(scheduled.cron()).next(next);

        // Assert
        assertEquals("America/Chicago", scheduled.zone());
        assertEquals(DayOfWeek.TUESDAY, next.getDayOfWeek());
        assertEquals(ZonedDateTime.of(2026, 11, 17, 14, 0, 0, 0, central), next);
        assertEquals(next.plusWeeks(1), oneAfter);
    }
}
