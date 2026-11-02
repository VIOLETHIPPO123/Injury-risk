package com.sidelined.backend.repository;

import com.sidelined.backend.model.Injury;
import com.sidelined.backend.model.InjuryStatus;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// The sample data is hard-coded, so these tests guard the data itself.
class InMemoryInjuryRepositoryTest {

    private final InMemoryInjuryRepository injuryRepository = new InMemoryInjuryRepository();

    @Test
    void findByPlayerId_returnsOnlyThatPlayersInjuries() {
        List<Injury> injuries = injuryRepository.findByPlayerId(321L);

        assertEquals(4, injuries.size());
        assertTrue(injuries.stream().allMatch(injury -> injury.playerId().equals(321L)));
    }

    @Test
    void findByPlayerId_playerWithoutInjuries_returnsEmpty() {
        assertTrue(injuryRepository.findByPlayerId(1L).isEmpty());
    }

    @Test
    void findByPlayerId_includesARepeatedInjury() {
        // Christian McCaffrey's two hamstring injuries are what the repeat warning is shown for.
        long hamstrings = injuryRepository.findByPlayerId(321L).stream()
            .filter(injury -> injury.bodyPart().equals("Hamstring"))
            .count();

        assertEquals(2, hamstrings);
    }

    @Test
    void hasActiveInjury_playerWithAnActiveInjury_isTrue() {
        // Mark Andrews: one Active injury. Tee Higgins: a Recovered one and an Active one.
        assertTrue(injuryRepository.hasActiveInjury(28L));
        assertTrue(injuryRepository.hasActiveInjury(70L));
    }

    @Test
    void hasActiveInjury_onlyRecoveringOrRecoveredInjuries_isFalse() {
        // Christian McCaffrey: Recovered and Recovering, nothing Active.
        assertFalse(injuryRepository.hasActiveInjury(321L));
    }

    @Test
    void hasActiveInjury_playerWithoutInjuries_isFalse() {
        assertFalse(injuryRepository.hasActiveInjury(1L));
    }

    @Test
    void findById_returnsThatInjury() {
        Injury injury = injuryRepository.findById(13L).orElseThrow();

        assertEquals(28L, injury.playerId());
        assertEquals(InjuryStatus.ACTIVE, injury.status());
        assertTrue(injuryRepository.findById(999L).isEmpty());
    }

    @Test
    void everyInjury_belongsToARealPlayer() {
        InMemoryPlayerRepository players = new InMemoryPlayerRepository();

        for (Injury injury : injuryRepository.findAll()) {
            assertTrue(players.existsById(injury.playerId()), "No player for " + injury);
        }
    }

    @Test
    void everyInjuryHasSaneValues() {
        for (Injury injury : injuryRepository.findAll()) {
            assertNotNull(injury.id(), "Missing id: " + injury);
            assertTrue(injury.week() >= 1 && injury.week() <= 18, "Week out of range: " + injury);
            assertTrue(injury.gamesMissed() >= 0, "Negative games missed: " + injury);
            assertFalse(injury.bodyPart().isBlank(), "Missing body part: " + injury);
            assertFalse(injury.type().isBlank(), "Missing type: " + injury);
            assertNotNull(injury.status(), "Missing status: " + injury);
        }
    }
}
