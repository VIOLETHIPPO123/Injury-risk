package com.sidelined.backend.service;

import com.sidelined.backend.model.Player;
import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Position;
import com.sidelined.backend.model.Workload;
import com.sidelined.backend.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// The service serves whatever its repository holds, wherever that data comes from.
class PlayerServiceTest {

    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final PlayerService playerService = new PlayerService(playerRepository);

    @Test
    void getAllPlayers_returnsWhatTheRepositoryHolds() {
        // Arrange
        List<Player> stored = List.of(new Player(1L, "Jacoby Brissett", NflTeam.ARI, Position.QB, new Workload(68, 253)));
        when(playerRepository.findAll()).thenReturn(stored);

        // Act
        List<Player> result = playerService.getAllPlayers();

        // Assert
        assertEquals(stored, result);
    }
}
