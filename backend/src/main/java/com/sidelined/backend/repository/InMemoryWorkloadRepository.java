package com.sidelined.backend.repository;

import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Player;
import com.sidelined.backend.model.WorkloadEntry;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

// Hard-coded stand-in until the external API module. This game-by-game history is SAMPLE
// DATA, generated from each player's stored snap counts so the two always agree: the last
// game is snapsLastGame and the last four games add up to snapsLast4Games. That keeps the
// newest weekly ACWR equal to the one on the player page. Older games come from a seeded
// random generator, so every run produces the same history.
@Repository
public class InMemoryWorkloadRepository implements WorkloadRepository {

    // Weeks played so far in the 2026-27 season.
    static final int CURRENT_WEEK = 8;
    // About the most snaps one player gets in a game, offense or defense.
    static final int MAX_SNAPS_PER_GAME = 100;
    // Fixed so the generated history is the same every run.
    private static final long SEED = 2026;

    private final Map<Long, List<WorkloadEntry>> byPlayerId;

    public InMemoryWorkloadRepository(PlayerRepository playerRepository) {
        this.byPlayerId = playerRepository.findAll().stream()
            .collect(Collectors.toMap(Player::getId, InMemoryWorkloadRepository::generate));
    }

    // Sample bye weeks: weeks 5 to 14, spread across the teams in enum order.
    static int byeWeek(NflTeam team) {
        return 5 + team.ordinal() % 10;
    }

    @Override
    public List<WorkloadEntry> findByPlayerId(Long playerId) {
        return byPlayerId.getOrDefault(playerId, List.of());
    }

    // Weeks 1 to CURRENT_WEEK, skipping the team's bye.
    private static List<WorkloadEntry> generate(Player player) {
        int bye = byeWeek(player.getTeam());
        List<Integer> weeks = new ArrayList<>();
        for (int week = 1; week <= CURRENT_WEEK; week++) {
            if (week != bye) weeks.add(week);
        }

        int[] snaps = snapsPerGame(player, weeks.size());
        List<WorkloadEntry> entries = new ArrayList<>();
        for (int i = 0; i < weeks.size(); i++) {
            entries.add(new WorkloadEntry(player.getId(), weeks.get(i), snaps[i]));
        }
        return entries;
    }

    // Snaps for each game, oldest first. The last four come from the stored snap counts:
    // the last game as stored, and the other three split the rest of the four-game total as
    // evenly as whole snaps allow. Older games are the four-game average, moved by a random
    // -10% to +10%. Each player has their own seed, so adding a player changes no one else.
    private static int[] snapsPerGame(Player player, int games) {
        int lastGame = player.getSnapsLastGame();
        int lastFour = player.getSnapsLast4Games();
        int otherThree = lastFour - lastGame;
        int[] snaps = new int[games];

        for (int i = 0; i < 3; i++) {
            snaps[games - 4 + i] = otherThree / 3 + (i < otherThree % 3 ? 1 : 0);
        }
        snaps[games - 1] = lastGame;

        Random random = new Random(SEED + player.getId());
        double average = lastFour / 4.0;
        for (int i = 0; i < games - 4; i++) {
            int generated = (int) Math.round(average * (0.9 + 0.2 * random.nextDouble()));
            snaps[i] = Math.min(generated, MAX_SNAPS_PER_GAME);
        }
        return snaps;
    }
}
