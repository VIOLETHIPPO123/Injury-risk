package com.sidelined.backend;

import com.sidelined.backend.model.InjuryReport;
import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Player;
import com.sidelined.backend.model.Position;
import com.sidelined.backend.model.Workload;
import com.sidelined.backend.repository.InjuryReportRepository;
import com.sidelined.backend.repository.JpaPlayerRepository;
import com.sidelined.backend.service.InjuryReportSyncJob;
import com.sidelined.backend.service.NflverseClient;
import com.sidelined.backend.service.NflverseInjuryReport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

// The injury report job against real tables (the in-memory H2 database of the "test" profile),
// with the download mocked. Each test runs in a transaction that is rolled back afterwards.
// flushAndClear() writes everything to the tables and forgets it, so the next read comes from
// the database, as it would the next night.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InjuryReportSyncPersistenceTest {

    // Ids no other test uses, since tests share the database.
    private static final String HURTS = "00-INJ-01";
    private static final String BROWN = "00-INJ-02";
    private static final String NOBODY = "00-INJ-99";

    // The job uses the real date, so the reports are put in the season it is loading now:
    // this year's from September on, last year's before that.
    private static final int SEASON = currentSeason();

    @Autowired private InjuryReportSyncJob job;
    @Autowired private InjuryReportRepository injuryReportRepository;
    @Autowired private JpaPlayerRepository playerRepository;
    @Autowired private EntityManager entityManager;

    @MockitoBean private NflverseClient nflverse;

    private Player hurts;
    private Player brown;

    private static int currentSeason() {
        LocalDate today = LocalDate.now(ZoneId.of("America/Chicago"));
        return today.getMonthValue() >= 9 ? today.getYear() : today.getYear() - 1;
    }

    @BeforeEach
    void storePlayers() {
        hurts = playerRepository.save(new Player(null, HURTS, "Jalen Hurts", NflTeam.PHI, Position.QB, new Workload(0, 0)));
        brown = playerRepository.save(new Player(null, BROWN, "A.J. Brown", NflTeam.PHI, Position.WR, new Workload(0, 0)));
        when(nflverse.injuryReports(anyInt())).thenReturn(Optional.empty());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private void nflverseHas(NflverseInjuryReport... reports) {
        when(nflverse.injuryReports(SEASON)).thenReturn(Optional.of(List.of(reports)));
    }

    private static NflverseInjuryReport report(String gsisId, int week, String gameStatus) {
        return new NflverseInjuryReport(gsisId, SEASON, week, "Hamstring", null, gameStatus,
            "Did Not Participate In Practice", null);
    }

    private List<InjuryReport> reportsOf(Player player) {
        return injuryReportRepository.findBySeasonIn(List.of(SEASON)).stream()
            .filter(report -> report.getPlayer().getId().equals(player.getId()))
            .toList();
    }

    @Test
    void runningTwice_addsNoDuplicates_andKeepsEveryId() {
        // Arrange
        nflverseHas(report(HURTS, 1, "Questionable"), report(HURTS, 2, "Out"), report(BROWN, 2, "Out"));
        long before = injuryReportRepository.count();

        // Act
        InjuryReportSyncJob.Result first = job.sync();
        flushAndClear();
        List<Long> ids = reportsOf(hurts).stream().map(InjuryReport::getId).sorted().toList();
        InjuryReportSyncJob.Result second = job.sync();
        flushAndClear();

        // Assert
        assertEquals(new InjuryReportSyncJob.Result(3, 0, 0), first);
        assertEquals(new InjuryReportSyncJob.Result(0, 0, 0), second);
        assertEquals(before + 3, injuryReportRepository.count());
        assertEquals(ids, reportsOf(hurts).stream().map(InjuryReport::getId).sorted().toList());
    }

    @Test
    void aLaterRun_updatesTheWeeksRow_andLinksItToThePlayer() {
        // Arrange
        nflverseHas(report(BROWN, 5, "Questionable"));
        job.sync();
        flushAndClear();
        Long id = reportsOf(brown).get(0).getId();

        // Act: the next night Brown has been ruled out.
        nflverseHas(report(BROWN, 5, "Out"));
        InjuryReportSyncJob.Result result = job.sync();
        flushAndClear();

        // Assert
        assertEquals(new InjuryReportSyncJob.Result(0, 1, 0), result);
        InjuryReport stored = reportsOf(brown).get(0);
        assertEquals(1, reportsOf(brown).size());
        assertEquals(id, stored.getId());
        assertEquals("Out", stored.getGameStatus());
        assertEquals("A.J. Brown", stored.getPlayer().getName());
        assertEquals(5, stored.getWeek());
    }

    @Test
    void reportForAnUnknownPlayer_isCountedNotStored() {
        nflverseHas(report(NOBODY, 1, "Out"), report(HURTS, 1, "Out"));
        long before = injuryReportRepository.count();

        InjuryReportSyncJob.Result result = job.sync();
        flushAndClear();

        assertEquals(new InjuryReportSyncJob.Result(1, 0, 1), result);
        assertEquals(before + 1, injuryReportRepository.count());
    }

    @Test
    void theTableAllowsOneRowPerPlayerPerWeek() {
        injuryReportRepository.saveAndFlush(new InjuryReport(hurts, SEASON, 7, "Knee", null, "Out", null));

        assertThrows(DataIntegrityViolationException.class, () -> injuryReportRepository.saveAndFlush(
            new InjuryReport(hurts, SEASON, 7, "Knee", null, "Questionable", null)));
    }
}
