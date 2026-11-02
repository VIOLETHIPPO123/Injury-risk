package com.sidelined.backend.repository;

import com.sidelined.backend.model.Injury;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// The sample data is hard-coded, so these tests guard the data itself.
class InMemoryInjuryRepositoryTest {

    // Every player id that has sample injuries.
    private static final List<Long> SAMPLE_PLAYER_IDS = List.of(321L, 13L, 69L, 92L, 173L, 91L, 28L, 70L);

    private final InMemoryInjuryRepository injuryRepository = new InMemoryInjuryRepository();

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
    void findByPlayerId_includesAnActiveInjury() {
        // Mark Andrews is hurt now, so he's someone who can go on IR.
        assertTrue(injuryRepository.findByPlayerId(28L).stream()
            .anyMatch(injury -> injury.status().equals("Active")));
    }

    @Test
    void everySamplePlayerIsARealPlayer() {
        InMemoryPlayerRepository players = new InMemoryPlayerRepository();

        for (Long id : SAMPLE_PLAYER_IDS) {
            assertTrue(players.existsById(id), "No player with id " + id);
            assertFalse(injuryRepository.findByPlayerId(id).isEmpty(), "No injuries for id " + id);
        }
    }

    @Test
    void everyInjuryHasSaneValues() {
        for (Long id : SAMPLE_PLAYER_IDS) {
            for (Injury injury : injuryRepository.findByPlayerId(id)) {
                assertTrue(injury.week() >= 1 && injury.week() <= 18, "Week out of range: " + injury);
                assertTrue(injury.gamesMissed() >= 0, "Negative games missed: " + injury);
                assertFalse(injury.bodyPart().isBlank(), "Missing body part: " + injury);
                assertFalse(injury.type().isBlank(), "Missing type: " + injury);
                assertFalse(injury.status().isBlank(), "Missing status: " + injury);
            }
        }
    }
}
