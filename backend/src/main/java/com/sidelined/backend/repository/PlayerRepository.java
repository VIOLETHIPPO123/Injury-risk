package com.sidelined.backend.repository;

import com.sidelined.backend.model.Player;

// Read-only: players will come from an external API. Today: InMemoryPlayerRepository.
public interface PlayerRepository extends ReadOnlyRepository<Player, Long> {
}
