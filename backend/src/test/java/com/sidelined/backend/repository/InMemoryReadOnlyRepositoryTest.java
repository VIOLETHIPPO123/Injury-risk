package com.sidelined.backend.repository;

import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Team;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

// The shared lookup code, tested once on its own data instead of through each repository.
class InMemoryReadOnlyRepositoryTest {

    // The smallest possible subclass: a list and which field is the id.
    private static class TestRepository extends InMemoryReadOnlyRepository<Team, NflTeam> {
        TestRepository(List<Team> teams) {
            super(teams, Team::getCode);
        }
    }

    private final List<Team> teams = List.of(
        new Team(NflTeam.ARI, "Arizona Cardinals"),
        new Team(NflTeam.ATL, "Atlanta Falcons")
    );

    @Test
    void findAll_returnsItemsInTheOrderGiven() {
        // Arrange
        TestRepository repository = new TestRepository(teams);

        // Act
        List<Team> result = repository.findAll();

        // Assert
        assertEquals(teams, result);
    }

    @Test
    void findById_existingId_returnsThatItem() {
        // Arrange
        TestRepository repository = new TestRepository(teams);

        // Act
        Optional<Team> result = repository.findById(NflTeam.ATL);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Atlanta Falcons", result.get().getName());
    }

    @Test
    void findById_missingId_returnsEmpty() {
        // Arrange
        TestRepository repository = new TestRepository(teams);

        // Act
        Optional<Team> result = repository.findById(NflTeam.BAL);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void existsById_answersForPresentAndMissingIds() {
        // Arrange
        TestRepository repository = new TestRepository(teams);

        // Act + Assert
        assertTrue(repository.existsById(NflTeam.ARI));
        assertFalse(repository.existsById(NflTeam.BAL));
    }

    @Test
    void constructor_duplicateId_failsFast() {
        // Arrange
        List<Team> withDuplicate = List.of(
            new Team(NflTeam.ARI, "Arizona Cardinals"),
            new Team(NflTeam.ARI, "Another Arizona")
        );

        // Act + Assert
        assertThrows(IllegalStateException.class, () -> new TestRepository(withDuplicate));
    }
}
