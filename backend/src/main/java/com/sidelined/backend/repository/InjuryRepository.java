package com.sidelined.backend.repository;

import com.sidelined.backend.model.Injury;
import java.util.List;

// Read-only and keyed by injury id: injury history will come from an external API.
// Today: InMemoryInjuryRepository.
public interface InjuryRepository extends ReadOnlyRepository<Injury, Long> {
    // A player's injuries, in no particular order; empty when they have none.
    List<Injury> findByPlayerId(Long playerId);

    // Whether the player is hurt right now, which is what lets them go on IR.
    boolean hasActiveInjury(Long playerId);
}
