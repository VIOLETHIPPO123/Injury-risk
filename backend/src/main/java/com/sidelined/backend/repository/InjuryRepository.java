package com.sidelined.backend.repository;

import com.sidelined.backend.model.Injury;
import java.util.List;

// A player's injuries. Read-only: they are worked out from the weekly injury reports
// (ReportedInjuryRepository), not stored.
public interface InjuryRepository {
    // A player's injuries, in no particular order; empty when they have none.
    List<Injury> findByPlayerId(Long playerId);

    // Whether the player is hurt right now (an Active injury), which is what lets them go on IR.
    boolean hasActiveInjury(Long playerId);
}
