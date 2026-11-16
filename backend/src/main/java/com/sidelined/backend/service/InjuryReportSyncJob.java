package com.sidelined.backend.service;

import com.sidelined.backend.model.InjuryReport;
import com.sidelined.backend.model.Player;
import com.sidelined.backend.repository.InjuryReportRepository;
import com.sidelined.backend.repository.JpaPlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

// Brings the injury_reports table up to date with nflverse's weekly injury reports for the
// current season and the two before it. A report is matched to a player by gsis_id; one for a
// player who isn't in the players table (most often someone no longer on a depth chart) is
// logged and counted, not stored. Stored reports are updated in place, one row per player per
// week, so running twice adds nothing. Reports are never deleted, including older seasons.
@Service
public class InjuryReportSyncJob {

    private static final Logger log = LoggerFactory.getLogger(InjuryReportSyncJob.class);

    static final ZoneId CENTRAL = ZoneId.of("America/Chicago");

    // How many seasons are loaded, counting the current one.
    private static final int SEASONS = 3;

    // What one run did. Unmatched reports are for players not in the players table.
    public record Result(int added, int updated, int unmatched) {
    }

    private record Week(String gsisId, int season, int week) {
    }

    private record StoredWeek(Long playerId, int season, int week) {
    }

    private final NflverseClient nflverse;
    private final JpaPlayerRepository playerRepository;
    private final InjuryReportRepository injuryReportRepository;
    private final Clock clock;

    @Autowired
    public InjuryReportSyncJob(NflverseClient nflverse, JpaPlayerRepository playerRepository,
                               InjuryReportRepository injuryReportRepository) {
        this(nflverse, playerRepository, injuryReportRepository, Clock.system(CENTRAL));
    }

    // Tests pass a fixed clock.
    InjuryReportSyncJob(NflverseClient nflverse, JpaPlayerRepository playerRepository,
                        InjuryReportRepository injuryReportRepository, Clock clock) {
        this.nflverse = nflverse;
        this.playerRepository = playerRepository;
        this.injuryReportRepository = injuryReportRepository;
        this.clock = clock;
    }

    // Every day at 2am Central. A failure is logged and the table left as it was; tomorrow's
    // run (or an admin's) tries again.
    @Scheduled(cron = "0 0 2 * * *", zone = "America/Chicago")
    public void runDaily() {
        try {
            sync();
        } catch (RuntimeException e) {
            log.error("Daily injury report sync failed", e);
        }
    }

    // Also run at startup when the table is empty (NflverseDataLoader), and by admins on
    // demand. One run at a time, so two can't both add the same report. Every season is
    // downloaded before anything is saved, so a failed download (NflverseUnavailableException)
    // changes nothing.
    public synchronized Result sync() {
        List<Integer> seasons = seasonsToLoad(LocalDate.now(clock));
        List<NflverseInjuryReport> downloaded = new ArrayList<>();
        for (int season : seasons) {
            Optional<List<NflverseInjuryReport>> reports = nflverse.injuryReports(season);
            if (reports.isEmpty()) {
                log.warn("nflverse has no injury reports for the {} season yet", season);
            } else {
                downloaded.addAll(reports.get());
            }
        }

        Map<String, Player> players = new HashMap<>();
        for (Player player : playerRepository.findAll()) {
            if (player.getGsisId() != null) {
                players.put(player.getGsisId(), player);
            }
        }
        Map<StoredWeek, InjuryReport> stored = new HashMap<>();
        for (InjuryReport report : injuryReportRepository.findBySeasonIn(seasons)) {
            stored.put(new StoredWeek(report.getPlayer().getId(), report.getSeason(), report.getWeek()), report);
        }

        List<InjuryReport> toSave = new ArrayList<>();
        int added = 0;
        int updated = 0;
        int unmatched = 0;
        Set<String> unmatchedPlayers = new TreeSet<>();

        for (NflverseInjuryReport source : latestPerPlayerWeek(downloaded)) {
            Player player = players.get(source.gsisId());
            if (player == null) {
                log.debug("No player with gsis_id \"{}\" for their {} week {} injury report",
                    source.gsisId(), source.season(), source.week());
                unmatched++;
                unmatchedPlayers.add(source.gsisId());
                continue;
            }
            InjuryReport report = stored.get(new StoredWeek(player.getId(), source.season(), source.week()));
            if (report == null) {
                toSave.add(new InjuryReport(player, source.season(), source.week(), source.primaryInjury(),
                    source.secondaryInjury(), source.gameStatus(), source.practiceStatus()));
                added++;
            } else if (report.update(source.primaryInjury(), source.secondaryInjury(),
                    source.gameStatus(), source.practiceStatus())) {
                toSave.add(report);
                updated++;
            }
        }

        injuryReportRepository.saveAll(toSave);

        if (unmatched > 0) {
            log.warn("{} injury reports, for {} players, matched no player by gsis_id and weren't stored "
                + "(set this class's log level to DEBUG to see each one)", unmatched, unmatchedPlayers.size());
        }
        Result result = new Result(added, updated, unmatched);
        log.info("Injury report sync of seasons {} done: {}", seasons, result);
        return result;
    }

    // The current season and the two before it, oldest first. A season starts in September and
    // its playoffs run into the next year, so until September the current season is last year's.
    static List<Integer> seasonsToLoad(LocalDate today) {
        int current = today.getMonthValue() >= Month.SEPTEMBER.getValue() ? today.getYear() : today.getYear() - 1;
        List<Integer> seasons = new ArrayList<>();
        for (int season = current - SEASONS + 1; season <= current; season++) {
            seasons.add(season);
        }
        return seasons;
    }

    // nflverse sometimes lists a player twice in a week, when the NFL changed their status
    // (Questionable, then Out). Only the latest is kept: the newer modifiedAt, or the later
    // row in files without one.
    private static List<NflverseInjuryReport> latestPerPlayerWeek(List<NflverseInjuryReport> reports) {
        Map<Week, NflverseInjuryReport> latest = new LinkedHashMap<>();
        for (NflverseInjuryReport report : reports) {
            latest.merge(new Week(report.gsisId(), report.season(), report.week()), report,
                (earlier, later) -> isOlder(later, earlier) ? earlier : later);
        }
        return new ArrayList<>(latest.values());
    }

    private static boolean isOlder(NflverseInjuryReport a, NflverseInjuryReport b) {
        return a.modifiedAt() != null && b.modifiedAt() != null && a.modifiedAt().compareTo(b.modifiedAt()) < 0;
    }
}
