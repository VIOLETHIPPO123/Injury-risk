package com.sidelined.backend.service;

import com.sidelined.backend.model.Player;
import com.sidelined.backend.model.Workload;
import com.sidelined.backend.model.WorkloadEntry;
import com.sidelined.backend.repository.JpaPlayerRepository;
import com.sidelined.backend.repository.JpaWorkloadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

// Brings the workload_entries table up to date with nflverse's snap counts for the current
// season, then recalculates every player's two snap-count columns from what is stored.
// The whole season is read every run, not just the newest week, so a game nflverse publishes
// or corrects late is picked up the next time. A (player, season, week) that is already stored
// is updated in place, so a second run adds nothing.
@Service
public class WorkloadSyncJob {

    private static final Logger log = LoggerFactory.getLogger(WorkloadSyncJob.class);

    // What one run did. Unmatched counts the snap-count rows that belong to no stored player.
    public record Result(int added, int updated, int unmatched) {
    }

    // One player's game.
    private record Game(Long playerId, int season, int week) {
    }

    private final NflverseClient nflverse;
    private final JpaPlayerRepository playerRepository;
    private final JpaWorkloadRepository workloadRepository;

    public WorkloadSyncJob(NflverseClient nflverse, JpaPlayerRepository playerRepository,
                           JpaWorkloadRepository workloadRepository) {
        this.nflverse = nflverse;
        this.playerRepository = playerRepository;
        this.workloadRepository = workloadRepository;
    }

    // Run every Tuesday after the player sync (WeeklyNflverseSync), at startup when the table
    // is empty (NflverseDataLoader), and by admins on demand. One run at a time, so two can't
    // both add the same game. Throws NflverseUnavailableException, changing nothing, if a
    // download fails.
    public synchronized Result sync() {
        List<SnapCount> snapCounts = nflverse.snapCounts();
        if (snapCounts.isEmpty()) {
            log.info("Workload sync: nflverse has no snap counts yet");
            return new Result(0, 0, 0);
        }
        PfrIdMap ids = new PfrIdMap(nflverse.players());

        List<Player> players = playerRepository.findAll();
        Map<String, Player> playersByGsisId = new HashMap<>();
        Map<Long, Player> playersById = new HashMap<>();
        for (Player player : players) {
            playersById.put(player.getId(), player);
            if (player.getGsisId() != null) {
                playersByGsisId.put(player.getGsisId(), player);
            }
        }

        // Snaps for each stored player's game. Two rows for the same game are added together.
        Map<Game, Integer> snapsByGame = new LinkedHashMap<>();
        Map<String, Integer> unmatchedRowsByPlayer = new TreeMap<>();
        int season = 0;
        for (SnapCount row : snapCounts) {
            season = Math.max(season, row.season());
            Optional<String> gsisId = ids.gsisId(row.pfrId());
            Player player = gsisId.map(playersByGsisId::get).orElse(null);
            if (player == null) {
                String who = row.playerName() + " (" + row.pfrId() + "): "
                    + (gsisId.isEmpty() ? "not in nflverse's players file" : "not in the players table");
                unmatchedRowsByPlayer.merge(who, 1, Integer::sum);
                continue;
            }
            snapsByGame.merge(new Game(player.getId(), row.season(), row.week()), row.snaps(), Integer::sum);
        }

        Map<Game, WorkloadEntry> stored = new HashMap<>();
        for (WorkloadEntry entry : workloadRepository.findBySeason(season)) {
            stored.put(new Game(entry.getPlayerId(), entry.getSeason(), entry.getWeek()), entry);
        }

        List<WorkloadEntry> toSave = new ArrayList<>();
        int added = 0;
        int updated = 0;
        for (Map.Entry<Game, Integer> game : snapsByGame.entrySet()) {
            Game key = game.getKey();
            int snaps = game.getValue();
            WorkloadEntry entry = stored.get(key);
            if (entry == null) {
                entry = new WorkloadEntry(playersById.get(key.playerId()), key.season(), key.week(), snaps);
                stored.put(key, entry);
                toSave.add(entry);
                added++;
            } else if (entry.getSnaps() != snaps) {
                entry.setSnaps(snaps);
                toSave.add(entry);
                updated++;
            }
        }
        workloadRepository.saveAll(toSave);

        int recalculated = recalculateSnapColumns(players, stored.values(), season);

        int unmatched = unmatchedRowsByPlayer.values().stream().mapToInt(Integer::intValue).sum();
        unmatchedRowsByPlayer.forEach((who, rows) -> log.info("Unmatched snap counts: {} row(s) for {}", rows, who));
        if (unmatched > 0) {
            log.warn("Workload sync: {} snap count row(s) for {} player(s) matched no stored player",
                unmatched, unmatchedRowsByPlayer.size());
        }
        Result result = new Result(added, updated, unmatched);
        log.info("Workload sync done for {}: {}, {} player(s) recalculated", season, result, recalculated);
        return result;
    }

    // Sets every player's snap-count columns from their games this season (see
    // Workload.fromGames), saving only the players whose numbers changed. A player with no
    // games this season, a team defense included, goes to zero. Returns how many changed.
    private int recalculateSnapColumns(List<Player> players, Iterable<WorkloadEntry> entries, int season) {
        // Each player's snaps by week, in week order.
        Map<Long, TreeMap<Integer, Integer>> history = new HashMap<>();
        for (WorkloadEntry entry : entries) {
            if (entry.getSeason() == season) {
                history.computeIfAbsent(entry.getPlayerId(), id -> new TreeMap<>())
                    .put(entry.getWeek(), entry.getSnaps());
            }
        }

        List<Player> changed = new ArrayList<>();
        for (Player player : players) {
            TreeMap<Integer, Integer> games = history.get(player.getId());
            List<Integer> snaps = games == null ? List.of() : List.copyOf(games.values());
            if (player.updateWorkload(Workload.fromGames(snaps))) {
                changed.add(player);
            }
        }
        playerRepository.saveAll(changed);
        return changed.size();
    }
}
