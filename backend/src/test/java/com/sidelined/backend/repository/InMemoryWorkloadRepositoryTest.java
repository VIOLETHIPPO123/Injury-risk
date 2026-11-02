package com.sidelined.backend.repository;

import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Player;
import com.sidelined.backend.model.WeeklySnaps;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

// The history is sample data built from the stored snap counts, so these tests guard that
// the two always agree.
class InMemoryWorkloadRepositoryTest {

    private final InMemoryPlayerRepository playerRepository = new InMemoryPlayerRepository();
    private final InMemoryWorkloadRepository workloadRepository =
        new InMemoryWorkloadRepository(playerRepository);

    private static List<Integer> gameSnaps(List<WeeklySnaps> weeks) {
        return weeks.stream().map(WeeklySnaps::snaps).filter(Objects::nonNull).toList();
    }

    @Test
    void teamWithAByeSoFar_hasThatWeekMarked() {
        // Arrange: Jacoby Brissett (id 1), ARI, bye in week 5. Stored: 68 last game, 253 over 4.

        // Act
        List<WeeklySnaps> weeks = workloadRepository.findByPlayerId(1L);

        // Assert
        assertEquals(List.of(
            WeeklySnaps.game(1, 60), WeeklySnaps.game(2, 63), WeeklySnaps.game(3, 66),
            WeeklySnaps.game(4, 62), WeeklySnaps.bye(5), WeeklySnaps.game(6, 62),
            WeeklySnaps.game(7, 61), WeeklySnaps.game(8, 68)), weeks);
    }

    @Test
    void teamWithNoByeYet_playsEveryWeek() {
        // Arrange: Bryce Young (id 45), CAR, bye in week 9. Stored: 65 last game, 270 over 4.

        // Act
        List<WeeklySnaps> weeks = workloadRepository.findByPlayerId(45L);

        // Assert
        assertEquals(List.of(61, 64, 68, 71, 69, 68, 68, 65), gameSnaps(weeks));
    }

    @Test
    void everyPlayer_lastFourGamesMatchTheStoredSnapCounts() {
        for (Player player : playerRepository.findAll()) {
            // Act
            List<WeeklySnaps> weeks = workloadRepository.findByPlayerId(player.getId());
            List<Integer> games = gameSnaps(weeks);
            List<Integer> lastFour = games.subList(games.size() - 4, games.size());

            // Assert
            String who = player.getName() + " (" + player.getId() + ")";
            assertEquals(player.getSnapsLastGame(), lastFour.get(3), who);
            assertEquals(player.getSnapsLast4Games(),
                lastFour.stream().mapToInt(Integer::intValue).sum(), who);
            assertTrue(games.stream().allMatch(snaps -> snaps >= 0), who);
        }
    }

    @Test
    void everyPlayer_hasOneEntryPerWeek_withOnlyTheTeamByeEmpty() {
        for (Player player : playerRepository.findAll()) {
            // Act
            List<WeeklySnaps> weeks = workloadRepository.findByPlayerId(player.getId());

            // Assert
            int bye = InMemoryWorkloadRepository.byeWeek(player.getTeam());
            assertEquals(InMemoryWorkloadRepository.LATEST_WEEK, weeks.size());
            for (int i = 0; i < weeks.size(); i++) {
                WeeklySnaps week = weeks.get(i);
                assertEquals(i + 1, week.week());
                assertEquals(week.week() == bye, week.snaps() == null);
            }
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
