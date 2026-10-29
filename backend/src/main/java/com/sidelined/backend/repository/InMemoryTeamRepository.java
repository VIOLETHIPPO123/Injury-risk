package com.sidelined.backend.repository;

import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Team;

import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

// Hard-coded stand-in until the external API module. Moved here unchanged from
// TeamService, which now only asks this repository for data.
@Repository
public class InMemoryTeamRepository implements TeamRepository {

    private static final List<Team> TEAMS = Arrays.stream(NflTeam.values())
    .map(t -> new Team(t.getCode(), t.getDisplayName()))
    .toList();

    // Lookups by code without scanning the whole list. Collectors.toMap also fails fast on a duplicate.
    private final Map<String, Team> byCode = TEAMS.stream()
        .collect(Collectors.toMap(Team::getCode, Function.identity()));

    // The original order, so GET responses are unchanged.
    @Override
    public List<Team> findAll() {
        return TEAMS;
    }

    @Override
    public Optional<Team> findById(String id) {
        return Optional.ofNullable(byCode.get(id));
    }

    @Override
    public boolean existsById(String id) {
        return byCode.containsKey(id);
    }
}