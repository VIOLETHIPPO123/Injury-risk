package com.sidelined.backend.service;

import com.sidelined.backend.exception.NflverseUnavailableException;
import com.sidelined.backend.model.InjuryReport;
import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Player;
import com.sidelined.backend.model.Position;
import com.sidelined.backend.model.Workload;
import com.sidelined.backend.repository.InjuryReportRepository;
import com.sidelined.backend.repository.JpaPlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

// The download (NflverseClient) and both tables are mocked: these check what the job decides to
// save. InjuryReportSyncPersistenceTest runs it against real tables.
class InjuryReportSyncJobTest {

    private static final String MAHOMES_ID = "00-0033873";
    private static final String KELCE_ID = "00-0030506";
    private static final String RETIRED_ID = "00-0019596";

    private final Player mahomes = new Player(1L, MAHOMES_ID, "Patrick Mahomes", NflTeam.KC, Position.QB, new Workload(0, 0));
    private final Player kelce = new Player(2L, KELCE_ID, "Travis Kelce", NflTeam.KC, Position.TE, new Workload(0, 0));

    private final NflverseClient nflverse = mock(NflverseClient.class);
    private final JpaPlayerRepository playerRepository = mock(JpaPlayerRepository.class);
    private final InjuryReportRepository injuryReportRepository = mock(InjuryReportRepository.class);
    // Mid-November 2026, Central time: the 2026 season is under way.
    private final Clock november2026 = Clock.fixed(Instant.parse("2026-11-17T08:00:00Z"), InjuryReportSyncJob.CENTRAL);
    private final InjuryReportSyncJob job =
        new InjuryReportSyncJob(nflverse, playerRepository, injuryReportRepository, november2026);

    // By default both players are stored, no reports are, and nflverse has no files.
    @BeforeEach
    void defaults() {
        when(playerRepository.findAll()).thenReturn(List.of(mahomes, kelce));
        when(injuryReportRepository.findBySeasonIn(any())).thenReturn(List.of());
        when(nflverse.injuryReports(anyInt())).thenReturn(Optional.empty());
    }

    private void nflverseHas(int season, NflverseInjuryReport... reports) {
        when(nflverse.injuryReports(season)).thenReturn(Optional.of(List.of(reports)));
    }

    private static NflverseInjuryReport report(String gsisId, int season, int week, String injury, String status) {
        return new NflverseInjuryReport(gsisId, season, week, injury, null, status,
            "Limited Participation in Practice", null);
    }

    @SuppressWarnings("unchecked")
    private List<InjuryReport> saved() {
        ArgumentCaptor<List<InjuryReport>> captor = ArgumentCaptor.forClass(List.class);
        verify(injuryReportRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    // ---------- Which seasons ----------

    @Test
    void loadsTheCurrentSeasonAndTheTwoBeforeIt() {
        job.sync();

        verify(nflverse).injuryReports(2024);
        verify(nflverse).injuryReports(2025);
        verify(nflverse).injuryReports(2026);
        verify(nflverse, times(3)).injuryReports(anyInt());
        verify(injuryReportRepository).findBySeasonIn(List.of(2024, 2025, 2026));
    }

    @Test
    void seasonsToLoad_untilSeptember_theCurrentSeasonIsLastYears() {
        // February is the Super Bowl of the season that started the September before.
        assertEquals(List.of(2024, 2025, 2026), InjuryReportSyncJob.seasonsToLoad(LocalDate.of(2027, 2, 9)));
        assertEquals(List.of(2024, 2025, 2026), InjuryReportSyncJob.seasonsToLoad(LocalDate.of(2027, 8, 31)));
        assertEquals(List.of(2025, 2026, 2027), InjuryReportSyncJob.seasonsToLoad(LocalDate.of(2027, 9, 1)));
    }

    @Test
    void runsEveryDayAt2amCentral() throws NoSuchMethodException {
        Scheduled schedule = InjuryReportSyncJob.class.getMethod("runDaily").getAnnotation(Scheduled.class);

        assertEquals("0 0 2 * * *", schedule.cron());
        assertEquals("America/Chicago", schedule.zone());
    }

    // ---------- Adding and updating ----------

    @Test
    void newReport_isAddedForThePlayerWithItsGsisId() {
        // Arrange
        nflverseHas(2026, new NflverseInjuryReport(MAHOMES_ID, 2026, 10, "Ankle", "Knee", "Questionable",
            "Limited Participation in Practice", null));

        // Act
        InjuryReportSyncJob.Result result = job.sync();

        // Assert
        InjuryReport added = saved().get(0);
        assertEquals(1, saved().size());
        assertNull(added.getId(), "the database gives a new report its id");
        assertSame(mahomes, added.getPlayer());
        assertEquals(2026, added.getSeason());
        assertEquals(10, added.getWeek());
        assertEquals("Ankle", added.getPrimaryInjury());
        assertEquals("Knee", added.getSecondaryInjury());
        assertEquals("Questionable", added.getGameStatus());
        assertEquals("Limited Participation in Practice", added.getPracticeStatus());
        assertEquals(new InjuryReportSyncJob.Result(1, 0, 0), result);
    }

    @Test
    void reportsFromEverySeason_areAdded() {
        nflverseHas(2024, report(KELCE_ID, 2024, 1, "Knee", "Out"));
        nflverseHas(2025, report(KELCE_ID, 2025, 1, "Knee", "Out"));
        nflverseHas(2026, report(KELCE_ID, 2026, 1, "Knee", "Out"));

        InjuryReportSyncJob.Result result = job.sync();

        assertEquals(List.of(2024, 2025, 2026), saved().stream().map(InjuryReport::getSeason).toList());
        assertEquals(new InjuryReportSyncJob.Result(3, 0, 0), result);
    }

    @Test
    void storedReportThatChanged_isUpdatedInItsRow() {
        // Arrange: yesterday Mahomes was Questionable; today he is Out.
        InjuryReport stored = new InjuryReport(mahomes, 2026, 10, "Ankle", null, "Questionable",
            "Limited Participation in Practice");
        when(injuryReportRepository.findBySeasonIn(any())).thenReturn(List.of(stored));
        nflverseHas(2026, report(MAHOMES_ID, 2026, 10, "Ankle", "Out"));

        // Act
        InjuryReportSyncJob.Result result = job.sync();

        // Assert
        assertEquals(List.of(stored), saved());
        assertSame(stored, saved().get(0), "the same row, not a new one");
        assertEquals("Out", stored.getGameStatus());
        assertEquals(new InjuryReportSyncJob.Result(0, 1, 0), result);
    }

    @Test
    void storedReportThatDidntChange_isNotSavedAgain() {
        // Arrange: a second run with the same data.
        InjuryReport stored = new InjuryReport(mahomes, 2026, 10, "Ankle", null, "Out",
            "Limited Participation in Practice");
        when(injuryReportRepository.findBySeasonIn(any())).thenReturn(List.of(stored));
        nflverseHas(2026, report(MAHOMES_ID, 2026, 10, "Ankle", "Out"));

        // Act
        InjuryReportSyncJob.Result result = job.sync();

        // Assert
        assertEquals(List.of(), saved());
        assertEquals(new InjuryReportSyncJob.Result(0, 0, 0), result);
    }

    @Test
    void sameWeekInAnotherSeason_isItsOwnRow() {
        InjuryReport stored = new InjuryReport(mahomes, 2025, 10, "Ankle", null, "Out",
            "Limited Participation in Practice");
        when(injuryReportRepository.findBySeasonIn(any())).thenReturn(List.of(stored));
        nflverseHas(2026, report(MAHOMES_ID, 2026, 10, "Ankle", "Out"));

        InjuryReportSyncJob.Result result = job.sync();

        assertEquals(2026, saved().get(0).getSeason());
        assertEquals(new InjuryReportSyncJob.Result(1, 0, 0), result);
    }

    // ---------- One row per player per week ----------

    @Test
    void playerListedTwiceInAWeek_keepsTheLatestChange() {
        // Arrange: nflverse lists the newer change first.
        nflverseHas(2024,
            new NflverseInjuryReport(KELCE_ID, 2024, 15, "Illness", null, "Out", null, "2024-12-15T14:17:06Z"),
            new NflverseInjuryReport(KELCE_ID, 2024, 15, "Illness", null, "Questionable", null, "2024-12-15T03:34:33Z"));

        // Act
        InjuryReportSyncJob.Result result = job.sync();

        // Assert
        assertEquals(1, saved().size());
        assertEquals("Out", saved().get(0).getGameStatus());
        assertEquals(new InjuryReportSyncJob.Result(1, 0, 0), result);
    }

    @Test
    void playerListedTwiceInAWeek_withoutModifiedTimes_keepsTheLaterRow() {
        nflverseHas(2026, report(KELCE_ID, 2026, 3, "Knee", "Questionable"), report(KELCE_ID, 2026, 3, "Knee", "Out"));

        job.sync();

        assertEquals(1, saved().size());
        assertEquals("Out", saved().get(0).getGameStatus());
    }

    // ---------- Reports that match no player ----------

    @Test
    void reportForAnUnknownGsisId_isCountedAndNotStored() {
        // Arrange: a player no longer on a depth chart, so not in the players table.
        nflverseHas(2024,
            report(RETIRED_ID, 2024, 1, "Knee", "Out"),
            report(RETIRED_ID, 2024, 2, "Knee", "Out"),
            report(MAHOMES_ID, 2024, 2, "Ankle", "Questionable"));

        // Act
        InjuryReportSyncJob.Result result = job.sync();

        // Assert
        assertEquals(List.of(mahomes), saved().stream().map(InjuryReport::getPlayer).toList());
        assertEquals(new InjuryReportSyncJob.Result(1, 0, 2), result);
    }

    @Test
    void teamDefenses_withNoGsisId_matchNoReport() {
        // A blank gsis_id must not match the DEF rows, whose gsis_id is null.
        when(playerRepository.findAll()).thenReturn(List.of(Player.teamDefense(NflTeam.KC)));
        nflverseHas(2026, report("", 2026, 1, "Knee", "Out"));

        InjuryReportSyncJob.Result result = job.sync();

        assertEquals(List.of(), saved());
        assertEquals(new InjuryReportSyncJob.Result(0, 0, 1), result);
    }

    // ---------- Failures ----------

    @Test
    void seasonNflverseHasNoFileFor_isSkipped() {
        // Early September, before the first injury report of the season.
        nflverseHas(2025, report(MAHOMES_ID, 2025, 1, "Ankle", "Out"));

        InjuryReportSyncJob.Result result = job.sync();

        assertEquals(new InjuryReportSyncJob.Result(1, 0, 0), result);
    }

    @Test
    void downloadFails_throwsAndSavesNothing() {
        // Arrange: 2024 downloads fine, 2025 doesn't.
        nflverseHas(2024, report(MAHOMES_ID, 2024, 1, "Ankle", "Out"));
        when(nflverse.injuryReports(2025)).thenThrow(new NflverseUnavailableException("down"));

        // Act and assert
        assertThrows(NflverseUnavailableException.class, job::sync);
        verify(injuryReportRepository, never()).saveAll(any());
    }

    @Test
    void dailyRun_logsAFailureInsteadOfThrowing() {
        when(nflverse.injuryReports(anyInt())).thenThrow(new NflverseUnavailableException("down"));

        assertDoesNotThrow(job::runDaily);
        verify(injuryReportRepository, never()).saveAll(any());
    }
}
