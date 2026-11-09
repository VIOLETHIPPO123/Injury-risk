package com.sidelined.backend.service;

import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Team;
import com.sidelined.backend.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Loading the teams is tested in TeamDataLoaderTest. Here we only check that the service
// serves whatever its repository holds, in code order.
class TeamServiceTest {

    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final TeamService teamService = new TeamService(teamRepository);

    @Test
    void getAllTeams_returnsWhatTheRepositoryHolds_sortedByCode() {
        // Arrange
        List<Team> stored = List.of(new Team(NflTeam.ARI, "Arizona Cardinals"));
        when(teamRepository.findAll(Sort.by("code"))).thenReturn(stored);

        // Act
        List<Team> result = teamService.getAllTeams();

        // Assert
        assertEquals(stored, result);
    }
}
