package com.sidelined.backend.service;

import com.sidelined.backend.exception.NflverseUnavailableException;
import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Player;
import com.sidelined.backend.model.Position;
import com.sidelined.backend.model.Workload;
import com.sidelined.backend.repository.JpaPlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// Brings the players table up to date with nflverse. The current players are everyone on a
// team's newest depth chart, which also gives their team and exact position (LT, FS); the
// players file gives their name. A player is matched on gsis_id, so a second run updates the
// same rows: nothing is duplicated and no id changes. Also keeps one DEF row per team.
// Nobody is ever deleted (a roster may still point at them), so a player who leaves the
// league keeps the last team and position they had.
@Service
public class PlayerSyncJob {

    private static final Logger log = LoggerFactory.getLogger(PlayerSyncJob.class);

    // Jobs a player does on top of their position: the holder is usually the punter and the
    // returners are receivers or backs. A player gets one of these only if it is all they do.
    private static final Set<Position> SECOND_JOBS = EnumSet.of(Position.H, Position.PR, Position.KR);

    // What one run did. Skipped players are on a depth chart but couldn't be stored.
    public record Result(int added, int updated, int skipped, int defensesAdded) {
    }

    private final NflverseClient nflverse;
    private final JpaPlayerRepository playerRepository;

    public PlayerSyncJob(NflverseClient nflverse, JpaPlayerRepository playerRepository) {
        this.nflverse = nflverse;
        this.playerRepository = playerRepository;
    }

    // Run every Tuesday (WeeklyNflverseSync), at startup when the table is empty
    // (NflverseDataLoader), and by admins on demand. One run at a time, so two can't both add the same new player. Throws
    // NflverseUnavailableException, changing nothing, if a download fails.
    public synchronized Result sync() {
        Map<String, NflversePlayer> known = nflverse.players().stream()
            .collect(Collectors.toMap(NflversePlayer::gsisId, p -> p, (first, second) -> first));
        Map<String, List<DepthChartEntry>> listings = nflverse.latestDepthChart().stream()
            .collect(Collectors.groupingBy(DepthChartEntry::gsisId, LinkedHashMap::new, Collectors.toList()));
        if (listings.isEmpty()) {
            throw new NflverseUnavailableException("nflverse's depth chart is empty");
        }

        Map<String, Player> stored = new HashMap<>();
        for (Player player : playerRepository.findAll()) {
            if (player.getGsisId() != null) {
                stored.put(player.getGsisId(), player);
            }
        }

        List<Player> toSave = new ArrayList<>();
        int added = 0;
        int updated = 0;
        int skipped = 0;

        for (Map.Entry<String, List<DepthChartEntry>> listing : listings.entrySet()) {
            String gsisId = listing.getKey();
            List<DepthChartEntry> entries = listing.getValue();
            NflversePlayer source = known.get(gsisId);
            if (gsisId.isBlank() || source == null || source.name().isBlank()) {
                log.warn("Skipping a {} depth chart player with gsis_id \"{}\": not in nflverse's players file",
                    entries.get(0).team(), gsisId);
                skipped++;
                continue;
            }
            Optional<NflTeam> team = NflverseMapping.team(entries.get(0).team());
            if (team.isEmpty()) {
                log.warn("Skipping {} ({}): unknown team \"{}\"", source.name(), gsisId, entries.get(0).team());
                skipped++;
                continue;
            }
            Optional<Position> position = positionOf(entries, source);
            if (position.isEmpty()) {
                log.warn("Skipping {} ({}): can't map position {} (depth chart) or \"{}\" (players file)",
                    source.name(), gsisId,
                    entries.stream().map(DepthChartEntry::position).distinct().toList(), source.position());
                skipped++;
                continue;
            }

            Player player = stored.get(gsisId);
            if (player == null) {
                // nflverse's players and depth chart files carry no snap counts.
                toSave.add(new Player(null, gsisId, source.name(), team.get(), position.get(), new Workload(0, 0)));
                added++;
            } else if (player.updateFromNflverse(source.name(), team.get(), position.get())) {
                toSave.add(player);
                updated++;
            }
        }

        List<Player> newDefenses = missingDefenses();
        toSave.addAll(newDefenses);
        playerRepository.saveAll(toSave);

        Result result = new Result(added, updated, skipped, newDefenses.size());
        log.info("Player sync done: {}", result);
        return result;
    }

    // The position from the player's best depth chart listing (rank 1 is the starter) that we
    // can map, passing over second jobs if they have a real position too. If no listing can
    // be mapped, the players file's position; if that can't be either, empty.
    private static Optional<Position> positionOf(List<DepthChartEntry> entries, NflversePlayer source) {
        List<Position> mapped = entries.stream()
            .sorted(Comparator.comparingInt(DepthChartEntry::rank))
            .map(entry -> NflverseMapping.position(entry.position()))
            .flatMap(Optional::stream)
            .toList();
        return mapped.stream().filter(position -> !SECOND_JOBS.contains(position)).findFirst()
            .or(() -> mapped.stream().findFirst())
            .or(() -> NflverseMapping.position(source.position()));
    }

    // A DEF row for each team that has none, so there are always exactly 32.
    private List<Player> missingDefenses() {
        Set<NflTeam> have = playerRepository.findByPosition(Position.DEF.name()).stream()
            .map(Player::getTeam)
            .collect(Collectors.toSet());
        List<Player> missing = new ArrayList<>();
        for (NflTeam team : NflTeam.values()) {
            if (!have.contains(team)) {
                missing.add(Player.teamDefense(team));
            }
        }
        return missing;
    }
}
