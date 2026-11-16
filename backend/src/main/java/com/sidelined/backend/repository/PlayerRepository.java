package com.sidelined.backend.repository;

import com.sidelined.backend.model.Player;

// Read-only to the API: players come from nflverse (PlayerSyncJob). Today: JpaPlayerRepository.
public interface PlayerRepository extends ReadOnlyRepository<Player, Long> {
}
