package com.sidelined.backend.repository;

import com.sidelined.backend.model.WorkloadEntry;
import java.util.List;

// Read-only to the API: game-by-game snaps come from nflverse (WorkloadSyncJob).
// Today: JpaWorkloadRepository.
public interface WorkloadRepository {
    // A player's games this season, in week order (no entry for a week they didn't play);
    // empty for an unknown player.
    List<WorkloadEntry> findByPlayerId(Long playerId);
}
