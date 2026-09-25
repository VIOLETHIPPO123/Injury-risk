package com.sidelined.backend.service;

import com.sidelined.backend.model.Team;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class TeamServiceTest {

    private final TeamService teamService = new TeamService();

    @Test
    void getAllTeams_returnsAll32NflTeams() {
        // Act
        List<Team> teams = teamService.getAllTeams();

        // Assert
        assertEquals(32, teams.size());
    }

    @Test
    void getAllTeams_everyCodeIsUnique() {
        // Arrange
        List<Team> teams = teamService.getAllTeams();

        // Act
        Set<String> uniqueCodes = teams.stream().map(Team::getCode).collect(Collectors.toSet());

        // Assert
        assertEquals(teams.size(), uniqueCodes.size());
    }

    @Test
    void getAllTeams_everyTeamHasACodeAndName() {
        // Act
        List<Team> teams = teamService.getAllTeams();

        // Assert
        for (Team team : teams) {
            assertFalse(team.getCode() == null || team.getCode().isBlank());
            assertFalse(team.getName() == null || team.getName().isBlank(), "Missing name for " + team.getCode());
        }
    }
}
