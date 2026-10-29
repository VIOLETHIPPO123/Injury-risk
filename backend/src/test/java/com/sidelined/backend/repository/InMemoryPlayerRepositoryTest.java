package com.sidelined.backend.repository;

import com.sidelined.backend.model.Player;
import com.sidelined.backend.model.Team;
import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Position;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

// The player data is hard-coded here, so these tests guard the data itself: the watchlist looks
// players up by id, the formation page expects a full lineup per team, and ACWR needs sane snaps.
class InMemoryPlayerRepositoryTest {

    private final InMemoryPlayerRepository playerRepository = new InMemoryPlayerRepository();

    @Test
    void findById_existingId_returnsThatPlayer() {
        // Act
        Optional<Player> result = playerRepository.findById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Jacoby Brissett", result.get().getName());
    }

    @Test
    void findById_missingId_returnsEmpty() {
        // Act
        Optional<Player> result = playerRepository.findById(9999L);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void existsById_knownAndUnknownIds() {
        // Act + Assert
        assertTrue(playerRepository.existsById(1L));
        assertFalse(playerRepository.existsById(9999L));
    }

    @Test
    void findAll_returnsPlayers() {
        // Act
        List<Player> players = playerRepository.findAll();

        // Assert
        assertFalse(players.isEmpty());
    }

    @Test
    void findAll_everyIdIsUnique() {
        // Arrange
        List<Player> players = playerRepository.findAll();

        // Act
        Set<Long> uniqueIds = players.stream().map(Player::getId).collect(Collectors.toSet());

        // Assert
        assertEquals(players.size(), uniqueIds.size(), "Duplicate player ids would break watchlist lookups");
    }

    @Test
    void findAll_everyPlayerHasNameTeamAndPosition() {
        // Act
        List<Player> players = playerRepository.findAll();

        // Assert
        for (Player player : players) {
            assertNotNull(player.getId());
            assertFalse(player.getName() == null || player.getName().isBlank(), "Missing name for id " + player.getId());
            assertNotNull(player.getTeam(), "Missing team for " + player.getName());
            assertNotNull(player.getPosition(), "Missing position for " + player.getName());
        }
    }

    @Test
    void findAll_everyPlayerBelongsToAKnownTeam() {
        // Arrange
        Set<String> teamCodes = new InMemoryTeamRepository().findAll().stream()
            .map(Team::getCode)
            .collect(Collectors.toSet());

        // Act
        List<Player> players = playerRepository.findAll();

        // Assert
        for (Player player : players) {
            assertTrue(teamCodes.contains(player.getTeam().getCode()),
                player.getName() + " is on unknown team " + player.getTeam());
        }
    }

    @Test
    void findAll_everyTeamHasAFullOffensiveLineup() {
        // Arrange — the formation page places one of each of these, plus three WRs. FB is optional.
        List<Position> singlePositions = List.of(
        Position.QB, Position.RB, Position.TE, Position.LT,
        Position.LG, Position.C, Position.RG, Position.RT);

        // Act
        Map<NflTeam, List<Player>> playersByTeam = playerRepository.findAll().stream()
            .collect(Collectors.groupingBy(Player::getTeam));

        // Assert
        for (Map.Entry<NflTeam, List<Player>> team : playersByTeam.entrySet()) {
            Map<Position, Long> positionCounts = team.getValue().stream()
                .collect(Collectors.groupingBy(Player::getPosition, Collectors.counting()));

            for (Position position : singlePositions) {
                assertEquals(1L, positionCounts.getOrDefault(position, 0L),
                    team.getKey() + " should have exactly one " + position);
            }
            assertEquals(3L, positionCounts.getOrDefault(Position.WR, 0L), team.getKey() + " should have three WRs");
        }
    }

    @Test
    void findAll_everyTeamHasAFullDefensiveLineup() {
        // Arrange — the formation page places two DEs, three CBs, one FS, and one SS. Number of LBs and DTs depends on formation, so we don't check those.
        List<Position> singlePositions = List.of(Position.FS, Position.SS);

        // Act
        Map<NflTeam, List<Player>> playersByTeam = playerRepository.findAll().stream()
            .collect(Collectors.groupingBy(Player::getTeam));

        // Assert
        for (Map.Entry<NflTeam, List<Player>> team : playersByTeam.entrySet()) {
            Map<Position, Long> positionCounts = team.getValue().stream()
                .collect(Collectors.groupingBy(Player::getPosition, Collectors.counting()));

            for (Position position : singlePositions) {
                assertEquals(1L, positionCounts.getOrDefault(position, 0L),
                    team.getKey() + " should have exactly one " + position);
            }
            assertEquals(2L, positionCounts.getOrDefault(Position.DE, 0L), team.getKey() + " should have two DEs");
            assertEquals(3L, positionCounts.getOrDefault(Position.CB, 0L), team.getKey() + " should have three CBs");
        }
    }

    @Test
    void findAll_everyTeamHasAFullSpecialTeamsLineup() {
        // Arrange — the formation page places one of each of these.
        List<Position> singlePositions = List.of(
            Position.PK, Position.P, Position.H,
            Position.PR, Position.KR, Position.LS);

        // Act
        Map<NflTeam, List<Player>> playersByTeam = playerRepository.findAll().stream()
            .collect(Collectors.groupingBy(Player::getTeam));

        // Assert
        for (Map.Entry<NflTeam, List<Player>> team : playersByTeam.entrySet()) {
            Map<Position, Long> positionCounts = team.getValue().stream()
                .collect(Collectors.groupingBy(Player::getPosition, Collectors.counting()));

            for (Position position : singlePositions) {
                assertEquals(1L, positionCounts.getOrDefault(position, 0L),
                    team.getKey() + " should have exactly one " + position);
            }
        }
    }

    @Test
    void findAll_snapCountsAreValidForAcwr() {
        // Act
        List<Player> players = playerRepository.findAll();

        // Assert — the last game is one of the last four, so it can't exceed their total.
        for (Player player : players) {
            assertTrue(player.getSnapsLastGame() >= 0, player.getName() + " has negative snaps");
            assertTrue(player.getSnapsLastGame() <= player.getSnapsLast4Games(),
                player.getName() + " has more snaps last game than in the last four games");
        }
    }
}
