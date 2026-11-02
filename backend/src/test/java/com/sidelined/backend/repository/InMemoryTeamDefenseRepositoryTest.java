package com.sidelined.backend.repository;

import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Player;
import com.sidelined.backend.model.RosterPosition;
import com.sidelined.backend.model.TeamDefense;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryTeamDefenseRepositoryTest {

    private final InMemoryTeamDefenseRepository defenseRepository = new InMemoryTeamDefenseRepository();

    @Test
    void everyTeam_hasExactlyOneDefense() {
        Map<NflTeam, Long> perTeam = defenseRepository.findAll().stream()
            .collect(Collectors.groupingBy(TeamDefense::team, Collectors.counting()));

        Map<NflTeam, Long> expected = Arrays.stream(NflTeam.values())
            .collect(Collectors.toMap(Function.identity(), t -> 1L));
        assertEquals(expected, perTeam);
    }

    @Test
    void noDefenseId_collidesWithAPlayerId() {
        InMemoryPlayerRepository playerRepository = new InMemoryPlayerRepository();

        for (TeamDefense defense : defenseRepository.findAll()) {
            assertFalse(playerRepository.existsById(defense.id()), "Id clash: " + defense.id());
        }
    }

    @Test
    void defenseIds_startAboveEveryPlayerId() {
        long maxPlayerId = new InMemoryPlayerRepository().findAll().stream()
            .mapToLong(Player::getId).max().orElseThrow();

        assertTrue(maxPlayerId < InMemoryTeamDefenseRepository.ID_BASE);
    }

    @Test
    void everyDefense_isADefWithAName() {
        List<TeamDefense> defenses = defenseRepository.findAll();

        for (TeamDefense defense : defenses) {
            assertEquals(RosterPosition.DEF, defense.position());
            assertEquals(defense.team().getDisplayName() + " D/ST", defense.name());
        }
    }
}
