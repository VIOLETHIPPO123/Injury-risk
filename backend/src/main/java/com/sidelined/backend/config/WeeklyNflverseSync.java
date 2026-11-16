package com.sidelined.backend.config;

import com.sidelined.backend.service.PlayerSyncJob;
import com.sidelined.backend.service.WorkloadSyncJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// The weekly schedule for both nflverse jobs. They share one schedule, instead of each having
// its own, because the order matters: the workload sync can only store snaps for players the
// player sync has already added.
@Component
public class WeeklyNflverseSync {

    private static final Logger log = LoggerFactory.getLogger(WeeklyNflverseSync.class);

    private final PlayerSyncJob playerSyncJob;
    private final WorkloadSyncJob workloadSyncJob;

    public WeeklyNflverseSync(PlayerSyncJob playerSyncJob, WorkloadSyncJob workloadSyncJob) {
        this.playerSyncJob = playerSyncJob;
        this.workloadSyncJob = workloadSyncJob;
    }

    // Tuesday 2pm Central: Monday Night Football's data is published by then, so the week's
    // Thursday, Sunday and Monday games are all in. A failure is logged and that job's tables
    // are left as they were; next week's run (or an admin's) tries again. The workload sync
    // still runs if the player sync failed, for the players already stored.
    @Scheduled(cron = "0 0 14 * * TUE", zone = "America/Chicago")
    public void run() {
        try {
            playerSyncJob.sync();
        } catch (RuntimeException e) {
            log.error("Weekly player sync failed", e);
        }
        try {
            workloadSyncJob.sync();
        } catch (RuntimeException e) {
            log.error("Weekly workload sync failed", e);
        }
    }
}
