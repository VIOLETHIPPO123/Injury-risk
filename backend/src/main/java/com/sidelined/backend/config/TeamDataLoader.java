package com.sidelined.backend.config;

import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Team;
import com.sidelined.backend.repository.TeamRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Fills the teams table at startup with the 32 teams in NflTeam. Only missing teams are added,
// so a restart adds nothing and the table always ends up with exactly one row per team.
@Component
public class TeamDataLoader implements ApplicationRunner {

    private final TeamRepository teamRepository;

    public TeamDataLoader(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        Set<NflTeam> stored = teamRepository.findAll().stream()
            .map(Team::getCode)
            .collect(Collectors.toSet());

        List<Team> missing = Arrays.stream(NflTeam.values())
            .filter(team -> !stored.contains(team))
            .map(team -> new Team(team, team.getDisplayName()))
            .toList();

        if (!missing.isEmpty()) {
            teamRepository.saveAll(missing);
        }
    }
}
