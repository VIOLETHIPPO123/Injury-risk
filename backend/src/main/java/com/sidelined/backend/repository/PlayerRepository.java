package com.sidelined.backend.repository;

import com.sidelined.backend.model.Player;

// Read-only to the API: players come from nflverse (DEV-102). Today: JpaPlayerRepository.
public interface PlayerRepository extends ReadOnlyRepository<Player, Long> {
}
