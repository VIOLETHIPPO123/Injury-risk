package com.sidelined.backend.repository;

import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Player;
import com.sidelined.backend.model.WeeklySnaps;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;

// Hard-coded stand-in until the external API module. This week-by-week history is SAMPLE
// DATA, built from each player's stored snap counts so the two always agree: the last game
// is snapsLastGame and the last four games add up to snapsLast4Games. That keeps the newest
// weekly ACWR equal to the one on the player page. Older games vary a little around the
// same average so the history isn't flat.
@Repository
public class InMemoryWorkloadRepository implements WorkloadRepository {

    // Weeks played so far in the 2026-27 season.
    static final int LATEST_WEEK = 8;

    private final PlayerRepository playerRepository;

    public InMemoryWorkloadRepository(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    // Sample bye weeks: weeks 5 to 14, spread across the teams in enum order.
    static int byeWeek(NflTeam team) {
        return 5 + team.ordinal() % 10;
    }

    @Override
    public List<WeeklySnaps> findByPlayerId(Long playerId) {
        return playerRepository.findById(playerId).map(InMemoryWorkloadRepository::history)
            .orElse(List.of());
    }

    private static List<WeeklySnaps> history(Player player) {
        int bye = byeWeek(player.getTeam());
        int games = bye <= LATEST_WEEK ? LATEST_WEEK - 1 : LATEST_WEEK;
        int[] snaps = snapsPerGame(player, games);

        List<WeeklySnaps> weeks = new ArrayList<>();
        int game = 0;
        for (int week = 1; week <= LATEST_WEEK; week++) {
            weeks.add(week == bye ? WeeklySnaps.bye(week) : WeeklySnaps.game(week, snaps[game++]));
        }
        return weeks;
    }

    // Snaps for each game, oldest first. The last four come from the stored snap counts:
    // the last game as stored, and the other three split the rest of the four-game total as
    // evenly as whole snaps allow.
    private static int[] snapsPerGame(Player player, int games) {
        int lastGame = player.getSnapsLastGame();
        int lastFour = player.getSnapsLast4Games();
        int otherThree = lastFour - lastGame;
        int[] snaps = new int[games];

        for (int i = 0; i < 3; i++) {
            snaps[games - 4 + i] = otherThree / 3 + (i < otherThree % 3 ? 1 : 0);
        }
        snaps[games - 1] = lastGame;

        // Older games: the four-game average, moved by -10% to +10%.
        double average = lastFour / 4.0;
        for (int i = 0; i < games - 4; i++) {
            int step = (int) ((player.getId() + i) % 5) - 2;
            snaps[i] = (int) Math.round(average * (1 + step * 0.05));
        }
        return snaps;
    }
}
