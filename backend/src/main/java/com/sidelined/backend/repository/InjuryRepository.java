package com.sidelined.backend.repository;

import com.sidelined.backend.model.Injury;
import java.util.List;

// Read-only: injury history will come from an external API. Today: InMemoryInjuryRepository.
public interface InjuryRepository {
    // A player's injuries, in no particular order; empty when they have none.
    List<Injury> findByPlayerId(Long playerId);
}
