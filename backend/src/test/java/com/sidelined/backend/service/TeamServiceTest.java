package com.sidelined.backend.service;

import com.sidelined.backend.model.Team;
import com.sidelined.backend.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// The data itself is tested in InMemoryTeamRepositoryTest. Here we only check that the service
// serves whatever its repository holds, without depending on where the data comes from.
class TeamServiceTest {

    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final TeamService teamService = new TeamService(teamRepository);

    @Test
    void getAllTeams_returnsWhatTheRepositoryHolds() {
        // Arrange
        List<Team> stored = List.of(new Team("ARI", "Arizona Cardinals"));
        when(teamRepository.findAll()).thenReturn(stored);

        // Act
        List<Team> result = teamService.getAllTeams();

        // Assert
        assertEquals(stored, result);
    }
}
