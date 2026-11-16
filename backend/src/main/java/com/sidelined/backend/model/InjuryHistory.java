package com.sidelined.backend.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

// Turns a player's weekly injury report rows into injuries. The rules:
//  - Rows for the same body part (primary injury) in back-to-back weeks are one injury. One
//    missing week in between (a bye) doesn't end it; two or more do, as does a new season or a
//    different body part.
//  - Games missed: the weeks of the injury the player was listed Out.
//  - Status: Active if the injury is on the current week's report as Out or Doubtful,
//    Recovering if it is on it with any other status, Recovered if it isn't on it.
// Rows that aren't an injury (no injury given, or "Not injury related - resting player") are
// left out.
public final class InjuryHistory {

    // The latest week any report is for: what "the current week's report" means.
    public record ReportWeek(int season, int week) {
    }

    // More than this many weeks between two rows (a gap of 2 or more missing weeks) starts a new injury.
    private static final int MAX_WEEKS_APART = 2;

    private static final String NOT_AN_INJURY = "not injury related";

    private InjuryHistory() {
    }

    // The player's injuries, oldest first. Each takes the id of its first report row, so it keeps
    // the same id as later weeks are added to it.
    public static List<Injury> of(Long playerId, List<InjuryReport> reports, ReportWeek current) {
        List<InjuryReport> rows = reports.stream()
            .filter(InjuryHistory::isInjury)
            .sorted(Comparator.comparingInt(InjuryReport::getSeason).thenComparingInt(InjuryReport::getWeek))
            .toList();

        List<Injury> injuries = new ArrayList<>();
        List<InjuryReport> spell = new ArrayList<>();
        for (InjuryReport row : rows) {
            if (!spell.isEmpty() && !continues(spell.get(spell.size() - 1), row)) {
                injuries.add(injury(playerId, spell, current));
                spell = new ArrayList<>();
            }
            spell.add(row);
        }
        if (!spell.isEmpty()) {
            injuries.add(injury(playerId, spell, current));
        }
        return injuries;
    }

    private static boolean isInjury(InjuryReport row) {
        return row.getPrimaryInjury() != null
            && !row.getPrimaryInjury().toLowerCase(Locale.ROOT).startsWith(NOT_AN_INJURY);
    }

    private static boolean continues(InjuryReport last, InjuryReport next) {
        return last.getSeason() == next.getSeason()
            && next.getWeek() - last.getWeek() <= MAX_WEEKS_APART
            && bodyPart(last).equals(bodyPart(next));
    }

    private static String bodyPart(InjuryReport row) {
        return row.getPrimaryInjury().trim().toLowerCase(Locale.ROOT);
    }

    private static Injury injury(Long playerId, List<InjuryReport> spell, ReportWeek current) {
        InjuryReport first = spell.get(0);
        InjuryReport last = spell.get(spell.size() - 1);
        int gamesMissed = (int) spell.stream().filter(row -> isStatus(row, "Out")).count();
        // nflverse's reports have no injury type ("Strain"), only the body part.
        return new Injury(first.getId(), playerId, first.getSeason(), first.getWeek(),
            first.getPrimaryInjury(), null, gamesMissed, status(last, current));
    }

    private static InjuryStatus status(InjuryReport last, ReportWeek current) {
        boolean onCurrentReport = current != null
            && last.getSeason() == current.season() && last.getWeek() == current.week();
        if (!onCurrentReport) {
            return InjuryStatus.RECOVERED;
        }
        return isStatus(last, "Out") || isStatus(last, "Doubtful") ? InjuryStatus.ACTIVE : InjuryStatus.RECOVERING;
    }

    private static boolean isStatus(InjuryReport row, String status) {
        return status.equalsIgnoreCase(row.getGameStatus());
    }
}
