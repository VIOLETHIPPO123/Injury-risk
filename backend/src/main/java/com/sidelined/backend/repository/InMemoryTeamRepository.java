package com.sidelined.backend.repository;

import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Team;

import org.springframework.stereotype.Repository;
import java.util.Arrays;
import java.util.List;

// Hard-coded stand-in until the external API module. The lookups live in
// InMemoryReadOnlyRepository; this class only holds the 32 teams, keyed by code.
@Repository
public class InMemoryTeamRepository
        extends InMemoryReadOnlyRepository<Team, String>
        implements TeamRepository {

    private static final List<Team> TEAMS = Arrays.stream(NflTeam.values())
        .map(t -> new Team(t.getCode(), t.getDisplayName()))
        .toList();

    public InMemoryTeamRepository() {
        super(TEAMS, Team::getCode);
    }
}
