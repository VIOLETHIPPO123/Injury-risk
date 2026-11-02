package com.sidelined.backend.repository;

import com.sidelined.backend.model.WeeklySnaps;
import java.util.List;

// Read-only: game-by-game snaps will come from an external API. Today: InMemoryWorkloadRepository.
public interface WorkloadRepository {
    // A player's season so far, one entry per week from week 1, oldest first; empty for an
    // unknown player.
    List<WeeklySnaps> findByPlayerId(Long playerId);
}
