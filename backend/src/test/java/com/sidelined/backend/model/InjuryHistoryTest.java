package com.sidelined.backend.model;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.ArrayList;
import java.util.List;

import static com.sidelined.backend.model.InjuryStatus.*;
import static org.junit.jupiter.api.Assertions.*;

// The rules that turn weekly injury report rows into injuries.
class InjuryHistoryTest {

    private static final Long PLAYER_ID = 7L;
    private static final Player PLAYER =
        new Player(PLAYER_ID, "00-0000007", "Test Player", NflTeam.KC, Position.WR, new Workload(0, 0));
    // The current week's report, unless a test says otherwise.
    private static final InjuryHistory.ReportWeek CURRENT = new InjuryHistory.ReportWeek(2026, 10);

    private long nextId = 100;

    private InjuryReport row(int season, int week, String injury, String gameStatus) {
        InjuryReport report = new InjuryReport(PLAYER, season, week, injury, null, gameStatus,
            "Did Not Participate In Practice");
        ReflectionTestUtils.setField(report, "id", nextId++);
        return report;
    }

    private static List<Injury> history(InjuryReport... rows) {
        return InjuryHistory.of(PLAYER_ID, List.of(rows), CURRENT);
    }

    // ---------- Grouping ----------

    @Test
    void backToBackWeeks_sameBodyPart_areOneInjury() {
        List<Injury> injuries = history(
            row(2025, 3, "Hamstring", "Questionable"),
            row(2025, 4, "Hamstring", "Out"),
            row(2025, 5, "Hamstring", "Out"));

        assertEquals(1, injuries.size());
        Injury hamstring = injuries.get(0);
        assertEquals(2025, hamstring.season());
        assertEquals(3, hamstring.week(), "an injury is dated by its first week");
        assertEquals("Hamstring", hamstring.bodyPart());
        assertEquals(PLAYER_ID, hamstring.playerId());
    }

    @Test
    void byeWeekInTheMiddle_doesntEndTheInjury() {
        // Week 7 is the team's bye: no report that week.
        List<Injury> injuries = history(
            row(2025, 5, "Ankle", "Out"),
            row(2025, 6, "Ankle", "Out"),
            row(2025, 8, "Ankle", "Out"),
            row(2025, 9, "Ankle", "Questionable"));

        assertEquals(1, injuries.size());
        assertEquals(5, injuries.get(0).week());
        assertEquals(3, injuries.get(0).gamesMissed(), "the bye isn't a game missed");
    }

    @Test
    void gapOfTwoOrMoreWeeks_startsANewInjury() {
        // Weeks 6 and 7 missing.
        List<Injury> injuries = history(
            row(2025, 5, "Knee", "Out"),
            row(2025, 8, "Knee", "Questionable"));

        assertEquals(List.of(5, 8), injuries.stream().map(Injury::week).toList());
    }

    @Test
    void differentBodyPart_startsANewInjury_evenTheNextWeek() {
        List<Injury> injuries = history(
            row(2025, 5, "Knee", "Out"),
            row(2025, 6, "Ankle", "Questionable"),
            row(2025, 7, "Knee", "Questionable"));

        assertEquals(List.of("Knee", "Ankle", "Knee"), injuries.stream().map(Injury::bodyPart).toList());
    }

    @Test
    void bodyPart_ignoresCaseAndSpaces() {
        List<Injury> injuries = history(row(2025, 5, "Knee", "Out"), row(2025, 6, " knee ", "Out"));

        assertEquals(1, injuries.size());
    }

    @Test
    void newSeason_startsANewInjury() {
        // The Super Bowl (week 22) and the next season's week 1.
        List<Injury> injuries = history(row(2024, 22, "Knee", "Out"), row(2025, 1, "Knee", "Out"));

        assertEquals(List.of(2024, 2025), injuries.stream().map(Injury::season).toList());
    }

    @Test
    void regularSeasonIntoPlayoffs_isOneInjury() {
        List<Injury> injuries = history(row(2025, 18, "Groin", "Out"), row(2025, 19, "Groin", "Out"));

        assertEquals(1, injuries.size());
    }

    @Test
    void rowsInAnyOrder_areSortedByWeekFirst() {
        List<Injury> injuries = history(
            row(2025, 6, "Hip", "Out"),
            row(2024, 2, "Hip", "Out"),
            row(2025, 5, "Hip", "Out"));

        assertEquals(List.of(2024, 2025), injuries.stream().map(Injury::season).toList());
        assertEquals(5, injuries.get(1).week());
    }

    @Test
    void injuryKeepsTheIdOfItsFirstRow() {
        InjuryReport first = row(2025, 5, "Knee", "Out");
        InjuryReport second = row(2025, 6, "Knee", "Out");

        assertEquals(first.getId(), history(second, first).get(0).id());
    }

    @Test
    void rowsThatArentAnInjury_areLeftOut() {
        List<Injury> injuries = history(
            row(2025, 5, "Not injury related - resting player", null),
            row(2025, 6, "Not Injury Related - personal matter", "Out"),
            row(2025, 7, null, "Out"),
            row(2025, 8, "Shoulder", "Questionable"));

        assertEquals(List.of("Shoulder"), injuries.stream().map(Injury::bodyPart).toList());
    }

    @Test
    void noRows_noInjuries() {
        assertEquals(List.of(), history());
    }

    @Test
    void typeIsNull_theReportsDontHaveOne() {
        assertNull(history(row(2025, 5, "Knee", "Out")).get(0).type());
    }

    // ---------- Games missed ----------

    @Test
    void gamesMissed_countsTheWeeksListedOut() {
        List<Injury> injuries = history(
            row(2025, 3, "Calf", "Questionable"),
            row(2025, 4, "Calf", "Out"),
            row(2025, 5, "Calf", "Doubtful"),
            row(2025, 6, "Calf", "Out"),
            row(2025, 7, "Calf", null));

        assertEquals(2, injuries.get(0).gamesMissed());
    }

    @Test
    void gamesMissed_neverListedOut_isZero() {
        assertEquals(0, history(row(2025, 3, "Calf", "Questionable")).get(0).gamesMissed());
    }

    // ---------- Status ----------

    @Test
    void onTheCurrentReport_asOut_isActive() {
        assertEquals(ACTIVE, history(row(2026, 9, "Ankle", "Questionable"), row(2026, 10, "Ankle", "Out"))
            .get(0).status());
    }

    @Test
    void onTheCurrentReport_asDoubtful_isActive() {
        assertEquals(ACTIVE, history(row(2026, 10, "Ankle", "Doubtful")).get(0).status());
    }

    @Test
    void onTheCurrentReport_withAnyOtherStatus_isRecovering() {
        assertEquals(RECOVERING, history(row(2026, 9, "Ankle", "Out"), row(2026, 10, "Ankle", "Questionable"))
            .get(0).status());
        // Only on the practice report, with no game status.
        assertEquals(RECOVERING, history(row(2026, 10, "Ankle", null)).get(0).status());
    }

    @Test
    void notOnTheCurrentReport_isRecovered() {
        // Out last week; not listed this week.
        assertEquals(RECOVERED, history(row(2026, 9, "Ankle", "Out")).get(0).status());
        // Same week number, last season.
        assertEquals(RECOVERED, history(row(2025, 10, "Ankle", "Out")).get(0).status());
    }

    @Test
    void onlyTheInjuryOnTheCurrentReport_isntRecovered() {
        // An old knee injury, then a new ankle injury this week.
        List<Injury> injuries = history(
            row(2026, 2, "Knee", "Out"),
            row(2026, 10, "Ankle", "Out"));

        assertEquals(List.of(RECOVERED, ACTIVE), injuries.stream().map(Injury::status).toList());
    }

    @Test
    void onTheCurrentReport_butNotForAnInjury_leavesTheLastInjuryRecovered() {
        // Out last week with a knee injury; this week only resting.
        List<Injury> injuries = history(
            row(2026, 9, "Knee", "Out"),
            row(2026, 10, "Not injury related - resting player", null));

        assertEquals(List.of(RECOVERED), injuries.stream().map(Injury::status).toList());
    }

    @Test
    void noReportsAtAll_everythingIsRecovered() {
        List<InjuryReport> rows = new ArrayList<>(List.of(row(2026, 10, "Knee", "Out")));

        assertEquals(RECOVERED, InjuryHistory.of(PLAYER_ID, rows, null).get(0).status());
    }
}
