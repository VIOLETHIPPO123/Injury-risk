package com.sidelined.backend.repository;

import com.sidelined.backend.model.WorkloadEntry;
import java.util.List;

// Read-only: game-by-game snaps will come from an external API. Today: InMemoryWorkloadRepository.
public interface WorkloadRepository {
    // A player's games so far, in week order (no entry for a bye week); empty for an
    // unknown player.
    List<WorkloadEntry> findByPlayerId(Long playerId);
}
