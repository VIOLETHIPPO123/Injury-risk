package com.sidelined.backend.repository;

import com.sidelined.backend.model.Team;

// Read-only and keyed by team code (e.g. "ARI"). Today: InMemoryTeamRepository.
public interface TeamRepository extends ReadOnlyRepository<Team, String> {
}
