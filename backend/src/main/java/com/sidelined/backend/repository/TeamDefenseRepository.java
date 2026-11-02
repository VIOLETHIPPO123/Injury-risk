package com.sidelined.backend.repository;

import com.sidelined.backend.model.TeamDefense;

// Read-only and keyed by id. Today: InMemoryTeamDefenseRepository.
public interface TeamDefenseRepository extends ReadOnlyRepository<TeamDefense, Long> {
}
