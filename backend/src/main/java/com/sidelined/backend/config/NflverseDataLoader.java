package com.sidelined.backend.config;

import com.sidelined.backend.repository.JpaPlayerRepository;
import com.sidelined.backend.repository.JpaWorkloadRepository;
import com.sidelined.backend.service.PlayerSyncJob;
import com.sidelined.backend.service.WorkloadSyncJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// At startup, fills the players table and then the workload_entries table from nflverse, each
// only if it is empty, so a new database doesn't wait until Tuesday for its first data. A
// table that has rows is left to the weekly job. Players go first: snaps can only be stored
// for players that exist. Turned off in tests (nflverse.sync-on-startup=false), which must not
// download anything.
@Component
@ConditionalOnProperty(name = "nflverse.sync-on-startup", havingValue = "true", matchIfMissing = true)
public class NflverseDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(NflverseDataLoader.class);

    private final JpaPlayerRepository playerRepository;
    private final JpaWorkloadRepository workloadRepository;
    private final PlayerSyncJob playerSyncJob;
    private final WorkloadSyncJob workloadSyncJob;

    public NflverseDataLoader(JpaPlayerRepository playerRepository, JpaWorkloadRepository workloadRepository,
                              PlayerSyncJob playerSyncJob, WorkloadSyncJob workloadSyncJob) {
        this.playerRepository = playerRepository;
        this.workloadRepository = workloadRepository;
        this.playerSyncJob = playerSyncJob;
        this.workloadSyncJob = workloadSyncJob;
    }

    // nflverse being down must not stop the app from starting.
    @Override
    public void run(ApplicationArguments args) {
        if (playerRepository.count() == 0) {
            try {
                playerSyncJob.sync();
            } catch (RuntimeException e) {
                log.error("Couldn't load players from nflverse at startup; the table is still empty", e);
            }
        }
        if (workloadRepository.count() == 0) {
            try {
                workloadSyncJob.sync();
            } catch (RuntimeException e) {
                log.error("Couldn't load snap counts from nflverse at startup; the table is still empty", e);
            }
        }
    }
}
