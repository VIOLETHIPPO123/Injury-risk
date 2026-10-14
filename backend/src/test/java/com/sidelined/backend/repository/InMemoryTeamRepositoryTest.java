package com.sidelined.backend.repository;

import com.sidelined.backend.model.Team;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryTeamRepositoryTest {

    private final InMemoryTeamRepository teamRepository = new InMemoryTeamRepository();

    @Test
    void findById_existingCode_returnsThatTeam() {
        // Act
        Optional<Team> result = teamRepository.findById("ARI");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Arizona Cardinals", result.get().getName());
    }

    @Test
    void findById_missingCode_returnsEmpty() {
        // Act
        Optional<Team> result = teamRepository.findById("XYZ");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findAll_returnsAll32NflTeams() {
        // Act
        List<Team> teams = teamRepository.findAll();

        // Assert
        assertEquals(32, teams.size());
    }

    @Test
    void findAll_everyCodeIsUnique() {
        // Arrange
        List<Team> teams = teamRepository.findAll();

        // Act
        Set<String> uniqueCodes = teams.stream().map(Team::getCode).collect(Collectors.toSet());

        // Assert
        assertEquals(teams.size(), uniqueCodes.size());
    }

    @Test
    void findAll_everyTeamHasACodeAndName() {
        // Act
        List<Team> teams = teamRepository.findAll();

        // Assert
        for (Team team : teams) {
            assertFalse(team.getCode() == null || team.getCode().isBlank());
            assertFalse(team.getName() == null || team.getName().isBlank(), "Missing name for " + team.getCode());
        }
    }
}
