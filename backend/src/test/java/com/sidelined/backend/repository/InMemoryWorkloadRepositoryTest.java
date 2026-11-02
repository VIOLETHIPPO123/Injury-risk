package com.sidelined.backend.repository;

import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Player;
import com.sidelined.backend.model.WorkloadEntry;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

// The history is generated sample data, so these tests guard the generator: it agrees with
// the stored snap counts, repeats exactly, and stays within realistic numbers.
class InMemoryWorkloadRepositoryTest {

    private final InMemoryPlayerRepository playerRepository = new InMemoryPlayerRepository();
    private final InMemoryWorkloadRepository workloadRepository =
        new InMemoryWorkloadRepository(playerRepository);

    private static List<Integer> weeks(List<WorkloadEntry> entries) {
        return entries.stream().map(WorkloadEntry::week).toList();
    }

    private static List<Integer> snaps(List<WorkloadEntry> entries) {
        return entries.stream().map(WorkloadEntry::snaps).toList();
    }

    @Test
    void teamWithAByeSoFar_hasNoEntryForThatWeek() {
        // Arrange: Jacoby Brissett (id 1), ARI, bye in week 5. Stored: 68 last game, 253 over 4.

        // Act
        List<WorkloadEntry> entries = workloadRepository.findByPlayerId(1L);

        // Assert
        assertEquals(List.of(1, 2, 3, 4, 6, 7, 8), weeks(entries));
        assertEquals(List.of(62, 62, 61, 68), snaps(entries).subList(3, 7));
        assertTrue(entries.stream().allMatch(e -> e.playerId() == 1L));
    }

    @Test
    void teamWithNoByeYet_hasEveryWeek() {
        // Bryce Young (id 45), CAR, bye in week 9.
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8), weeks(workloadRepository.findByPlayerId(45L)));
    }

    @Test
    void everyPlayer_lastFourGamesMatchTheStoredSnapCounts() {
        for (Player player : playerRepository.findAll()) {
            // Act
            List<Integer> snaps = snaps(workloadRepository.findByPlayerId(player.getId()));
            List<Integer> lastFour = snaps.subList(snaps.size() - 4, snaps.size());

            // Assert
            String who = player.getName() + " (" + player.getId() + ")";
            assertEquals(player.getSnapsLastGame(), lastFour.get(3), who);
            assertEquals(player.getSnapsLast4Games(),
                lastFour.stream().mapToInt(Integer::intValue).sum(), who);
        }
    }

    @Test
    void everyPlayer_hasEveryWeekThroughCurrentWeekInOrder_exceptTheTeamBye() {
        for (Player player : playerRepository.findAll()) {
            // Arrange
            int bye = InMemoryWorkloadRepository.byeWeek(player.getTeam());
            List<Integer> expected = IntStream.rangeClosed(1, InMemoryWorkloadRepository.CURRENT_WEEK)
                .filter(week -> week != bye).boxed().toList();

            // Act + Assert
            assertEquals(expected, weeks(workloadRepository.findByPlayerId(player.getId())),
                player.getName());
        }
    }

    @Test
    void everyPlayer_snapsAreBetweenZeroAndTheRealisticMaximum() {
        for (Player player : playerRepository.findAll()) {
            for (int snaps : snaps(workloadRepository.findByPlayerId(player.getId()))) {
                assertTrue(snaps >= 0 && snaps <= InMemoryWorkloadRepository.MAX_SNAPS_PER_GAME,
                    player.getName() + ": " + snaps);
            }
        }
    }

    @Test
    void generatingAgain_producesTheSameData() {
        // Act
        InMemoryWorkloadRepository again = new InMemoryWorkloadRepository(new InMemoryPlayerRepository());

        // Assert
        for (Player player : playerRepository.findAll()) {
            assertEquals(workloadRepository.findByPlayerId(player.getId()),
                again.findByPlayerId(player.getId()), player.getName());
        }
    }

    @Test
    void byeWeeks_fallInWeeks5To14() {
        for (NflTeam team : NflTeam.values()) {
            int bye = InMemoryWorkloadRepository.byeWeek(team);
            assertTrue(bye >= 5 && bye <= 14, team + " bye " + bye);
        }
    }

    @Test
    void unknownPlayer_returnsEmptyList() {
        assertTrue(workloadRepository.findByPlayerId(9999L).isEmpty());
    }
}
