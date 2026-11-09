package com.sidelined.backend.repository;

import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

// The teams table, keyed by team code. Filled at startup by TeamDataLoader.
public interface TeamRepository extends JpaRepository<Team, NflTeam> {
}
