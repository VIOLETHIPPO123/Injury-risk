package com.sidelined.backend.validation;

import com.sidelined.backend.exception.SlotNotAllowedException;
import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Player;
import com.sidelined.backend.model.Position;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.model.Workload;
import com.sidelined.backend.repository.PlayerRepository;
import com.sidelined.backend.service.CurrentRosterFormat;
import org.junit.jupiter.api.Test;
import java.util.Optional;

import static com.sidelined.backend.model.StandardRosterFormats.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SlotAllowsPositionValidatorTest {

    // The signed-in user whose roster is being checked.
    private static final String USER = "user-a";

    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final CurrentRosterFormat currentFormat = mock(CurrentRosterFormat.class);
    private final SlotAllowsPositionValidator validator =
        new SlotAllowsPositionValidator(playerRepository, currentFormat);

    // ESPN Standard, the default format, and one player at each position the tests use.
    SlotAllowsPositionValidatorTest() {
        when(currentFormat.get(USER)).thenReturn(byId(ESPN_STANDARD).orElseThrow());
        store(1L, Position.QB);
        store(3L, Position.WR);
        store(6L, Position.TE);
        store(84L, Position.FB);
        store(791L, Position.PK);
        store(941L, Position.DEF);
    }

    private void store(long id, Position position) {
        when(playerRepository.findById(id)).thenReturn(Optional.of(
            new Player(id, "Player " + id, NflTeam.ARI, position, new Workload(10, 40))));
    }

    private static RosterEntry onBench(long playerId) {
        return new RosterEntry(1L, playerId, RosterEntry.BENCH, "");
    }

    @Test
    void validate_positionListedInTheSlot_passes() {
        assertDoesNotThrow(() -> validator.validate(USER, onBench(1L), "QB"));   // QB
        assertDoesNotThrow(() -> validator.validate(USER, onBench(6L), "FLEX")); // TE
        assertDoesNotThrow(() -> validator.validate(USER, onBench(84L), "RB"));  // FB counts as RB
        assertDoesNotThrow(() -> validator.validate(USER, onBench(791L), "K"));  // PK counts as K
        assertDoesNotThrow(() -> validator.validate(USER, onBench(941L), "DEF")); // a team defense
    }

    @Test
    void validate_positionNotListedInTheSlot_throws() {
        assertThrows(SlotNotAllowedException.class, () -> validator.validate(USER, onBench(1L), "RB"));
        assertThrows(SlotNotAllowedException.class, () -> validator.validate(USER, onBench(1L), "FLEX"));
        assertThrows(SlotNotAllowedException.class, () -> validator.validate(USER, onBench(3L), "DEF"));
        assertThrows(SlotNotAllowedException.class, () -> validator.validate(USER, onBench(941L), "FLEX"));
    }

    @Test
    void validate_benchAndIr_takeAnyone() {
        assertDoesNotThrow(() -> validator.validate(USER, onBench(1L), RosterEntry.BENCH));
        assertDoesNotThrow(() -> validator.validate(USER, onBench(1L), RosterEntry.IR));
    }

    @Test
    void validate_slotTheFormatDoesntHave_throws() {
        assertThrows(SlotNotAllowedException.class, () -> validator.validate(USER, onBench(1L), "SUPERFLEX"));
        assertThrows(SlotNotAllowedException.class, () -> validator.validate(USER, onBench(1L), null));
    }
}
