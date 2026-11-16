package com.sidelined.backend.config;

import com.sidelined.backend.repository.JpaPlayerRepository;
import com.sidelined.backend.service.PlayerSyncJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// At startup, fills the players table from nflverse if it is empty, so a new database doesn't
// wait until Tuesday for its first players. A table that has players is left to the weekly
// job. Turned off in tests (nflverse.sync-on-startup=false), which must not download anything.
@Component
@ConditionalOnProperty(name = "nflverse.sync-on-startup", havingValue = "true", matchIfMissing = true)
public class PlayerDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlayerDataLoader.class);

    private final JpaPlayerRepository playerRepository;
    private final PlayerSyncJob playerSyncJob;

    public PlayerDataLoader(JpaPlayerRepository playerRepository, PlayerSyncJob playerSyncJob) {
        this.playerRepository = playerRepository;
        this.playerSyncJob = playerSyncJob;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (playerRepository.count() > 0) {
            return;
        }
        // nflverse being down must not stop the app from starting.
        try {
            playerSyncJob.sync();
        } catch (RuntimeException e) {
            log.error("Couldn't load players from nflverse at startup; the table is still empty", e);
        }
    }
}
