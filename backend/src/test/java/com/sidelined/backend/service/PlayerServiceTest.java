package com.sidelined.backend.service;

import com.sidelined.backend.model.Player;
import com.sidelined.backend.model.Team;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

// PlayerService is hardcoded data, so these tests guard the data itself: the watchlist looks
// players up by id, the formation page expects a full lineup per team, and ACWR needs sane snaps.
class PlayerServiceTest {

    private final PlayerService playerService = new PlayerService();

    @Test
    void getAllPlayers_returnsPlayers() {
        // Act
        List<Player> players = playerService.getAllPlayers();

        // Assert
        assertFalse(players.isEmpty());
    }

    @Test
    void getAllPlayers_everyIdIsUnique() {
        // Arrange
        List<Player> players = playerService.getAllPlayers();

        // Act
        Set<Long> uniqueIds = players.stream().map(Player::getId).collect(Collectors.toSet());

        // Assert
        assertEquals(players.size(), uniqueIds.size(), "Duplicate player ids would break watchlist lookups");
    }

    @Test
    void getAllPlayers_everyPlayerHasNameTeamAndPosition() {
        // Act
        List<Player> players = playerService.getAllPlayers();

        // Assert
        for (Player player : players) {
            assertNotNull(player.getId());
            assertFalse(player.getName() == null || player.getName().isBlank(), "Missing name for id " + player.getId());
            assertFalse(player.getTeam() == null || player.getTeam().isBlank(), "Missing team for " + player.getName());
            assertFalse(player.getPosition() == null || player.getPosition().isBlank(), "Missing position for " + player.getName());
        }
    }

    @Test
    void getAllPlayers_everyPlayerBelongsToAKnownTeam() {
        // Arrange
        Set<String> teamCodes = new TeamService().getAllTeams().stream()
            .map(Team::getCode)
            .collect(Collectors.toSet());

        // Act
        List<Player> players = playerService.getAllPlayers();

        // Assert
        for (Player player : players) {
            assertTrue(teamCodes.contains(player.getTeam()),
                player.getName() + " is on unknown team " + player.getTeam());
        }
    }

    @Test
    void getAllPlayers_everyTeamHasAFullOffensiveLineup() {
        // Arrange — the formation page places one of each of these, plus three WRs. FB is optional.
        List<String> singlePositions = List.of("QB", "RB", "TE", "LT", "LG", "C", "RG", "RT");

        // Act
        Map<String, List<Player>> playersByTeam = playerService.getAllPlayers().stream()
            .collect(Collectors.groupingBy(Player::getTeam));

        // Assert
        for (Map.Entry<String, List<Player>> team : playersByTeam.entrySet()) {
            Map<String, Long> positionCounts = team.getValue().stream()
                .collect(Collectors.groupingBy(Player::getPosition, Collectors.counting()));

            for (String position : singlePositions) {
                assertEquals(1L, positionCounts.getOrDefault(position, 0L),
                    team.getKey() + " should have exactly one " + position);
            }
            assertEquals(3L, positionCounts.getOrDefault("WR", 0L), team.getKey() + " should have three WRs");
        }
    }

    @Test
    void getAllPlayers_snapCountsAreValidForAcwr() {
        // Act
        List<Player> players = playerService.getAllPlayers();

        // Assert — the last game is one of the last four, so it can't exceed their total.
        for (Player player : players) {
            assertTrue(player.getSnapsLastGame() >= 0, player.getName() + " has negative snaps");
            assertTrue(player.getSnapsLastGame() <= player.getSnapsLast4Games(),
                player.getName() + " has more snaps last game than in the last four games");
        }
    }
}
