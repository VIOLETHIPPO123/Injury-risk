package com.sidelined.backend.validation;

import com.sidelined.backend.exception.NotFantasyEligibleException;
import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Player;
import com.sidelined.backend.model.Position;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.model.Workload;
import com.sidelined.backend.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FantasyEligibleValidatorTest {

    // The signed-in user whose roster is being checked.
    private static final String USER = "user-a";

    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final FantasyEligibleValidator validator = new FantasyEligibleValidator(playerRepository);

    private static RosterEntry add(long playerId) {
        return new RosterEntry(null, playerId, null, "");
    }

    // A request to add a stored player who plays this position.
    private RosterEntry addPlayerAt(Position position) {
        long id = position.ordinal() + 1;
        when(playerRepository.findById(id)).thenReturn(Optional.of(
            new Player(id, "Player " + id, NflTeam.ARI, position, new Workload(10, 40))));
        return add(id);
    }

    @Test
    void validate_fantasyPositions_pass() {
        for (Position position : List.of(Position.QB, Position.RB, Position.WR, Position.TE,
                Position.FB, Position.PK, Position.DEF)) {
            RosterEntry request = addPlayerAt(position);
            assertDoesNotThrow(() -> validator.validate(USER, request), position.name());
        }
    }

    @Test
    void validate_ineligiblePositions_throwWithTheMessage() {
        for (Position position : List.of(Position.LT, Position.DE, Position.CB, Position.KR,
                Position.PR, Position.H, Position.P, Position.LS)) {
            RosterEntry request = addPlayerAt(position);
            NotFantasyEligibleException ex = assertThrows(NotFantasyEligibleException.class,
                () -> validator.validate(USER, request), position.name());
            assertEquals("Not a fantasy-eligible position.", ex.getMessage());
        }
    }

    @Test
    void validate_unknownPlayer_isLeftToPlayerExistsValidator() {
        when(playerRepository.findById(9999L)).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> validator.validate(USER, add(9999L)));
    }
}
