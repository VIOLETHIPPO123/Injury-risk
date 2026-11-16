package com.sidelined.backend;

import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Player;
import com.sidelined.backend.model.Position;
import com.sidelined.backend.model.Workload;
import com.sidelined.backend.model.WorkloadEntry;
import com.sidelined.backend.repository.JpaPlayerRepository;
import com.sidelined.backend.repository.JpaWorkloadRepository;
import com.sidelined.backend.service.NflverseClient;
import com.sidelined.backend.service.NflversePlayer;
import com.sidelined.backend.service.SnapCount;
import com.sidelined.backend.service.WorkloadService;
import com.sidelined.backend.service.WorkloadSyncJob;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

// The workload sync job against real tables (the in-memory H2 database of the "test" profile),
// with the download mocked. Each test runs in a transaction that is rolled back afterwards.
// flushAndClear() writes everything to the tables and forgets it, so the next read comes from
// the database, as it would the following Tuesday.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WorkloadSyncPersistenceTest {

    private static final int SEASON = 2026;
    // Ids no other test uses, since tests share the database.
    private static final String ALLEN_GSIS = "00-WORK-01";
    private static final String ALLEN_PFR = "WorkAl01";

    @Autowired private WorkloadSyncJob job;
    @Autowired private WorkloadService workloadService;
    @Autowired private JpaPlayerRepository playerRepository;
    @Autowired private JpaWorkloadRepository workloadRepository;
    @Autowired private EntityManager entityManager;

    @MockitoBean private NflverseClient nflverse;

    private Long allenId;

    @BeforeEach
    void storedPlayer() {
        allenId = playerRepository.save(
            new Player(null, ALLEN_GSIS, "Josh Allen", NflTeam.BUF, Position.QB, new Workload(0, 0))).getId();
        when(nflverse.players()).thenReturn(List.of(new NflversePlayer(ALLEN_GSIS, ALLEN_PFR, "Josh Allen", "QB")));
        flushAndClear();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    // Allen's games: one snap count per week, starting at week 1.
    private void nflverseHasAllensGames(int season, int... snapsByWeek) {
        List<SnapCount> snapCounts = new ArrayList<>();
        for (int i = 0; i < snapsByWeek.length; i++) {
            snapCounts.add(new SnapCount(ALLEN_PFR, "Josh Allen", season, i + 1, snapsByWeek[i]));
        }
        when(nflverse.snapCounts()).thenReturn(snapCounts);
    }

    private Player allen() {
        return playerRepository.findById(allenId).orElseThrow();
    }

    @Test
    void runningTwice_addsNoDuplicates() {
        // Arrange
        nflverseHasAllensGames(SEASON, 60, 62, 61, 70);
        long before = workloadRepository.count();

        // Act
        WorkloadSyncJob.Result first = job.sync();
        flushAndClear();
        WorkloadSyncJob.Result second = job.sync();
        flushAndClear();

        // Assert
        assertEquals(new WorkloadSyncJob.Result(4, 0, 0), first);
        assertEquals(new WorkloadSyncJob.Result(0, 0, 0), second);
        assertEquals(before + 4, workloadRepository.count());
        assertEquals(List.of(1, 2, 3, 4),
            workloadRepository.findByPlayerId(allenId).stream().map(WorkloadEntry::getWeek).toList());
    }

    @Test
    void nextTuesdaysRun_addsTheNewWeek_andStoresACorrection_inTheSameRows() {
        // Arrange
        nflverseHasAllensGames(SEASON, 60, 62, 61, 70);
        job.sync();
        flushAndClear();
        Long weekOneRow = workloadRepository.findByPlayerId(allenId).get(0).getId();

        // Act: week 5 is in, and week 1 has been corrected from 60 to 58.
        nflverseHasAllensGames(SEASON, 58, 62, 61, 70, 66);
        WorkloadSyncJob.Result result = job.sync();
        flushAndClear();

        // Assert
        assertEquals(new WorkloadSyncJob.Result(1, 1, 0), result);
        List<WorkloadEntry> games = workloadRepository.findByPlayerId(allenId);
        assertEquals(List.of(58, 62, 61, 70, 66), games.stream().map(WorkloadEntry::getSnaps).toList());
        assertEquals(weekOneRow, games.get(0).getId());
    }

    @Test
    void theDatabaseItself_refusesASecondRowForAPlayersWeek() {
        // Arrange
        nflverseHasAllensGames(SEASON, 60);
        job.sync();
        flushAndClear();

        // Act + Assert
        WorkloadEntry secondWeekOne = new WorkloadEntry(allen(), SEASON, 1, 99);
        assertThrows(DataIntegrityViolationException.class,
            () -> workloadRepository.saveAndFlush(secondWeekOne));
    }

    @Test
    void afterARun_thePlayersSnapColumns_matchTheLastPointOfTheirHistory() {
        // Arrange
        nflverseHasAllensGames(SEASON, 48, 52, 61, 40, 66, 58, 73);

        // Act
        job.sync();
        flushAndClear();

        // Assert: what GET /players shows, against what GET /players/{id}/workload returns.
        Player allen = allen();
        List<WorkloadEntry> history = workloadService.getWorkload(allenId);
        int lastGame = history.get(history.size() - 1).getSnaps();
        int lastFour = history.subList(history.size() - 4, history.size()).stream()
            .mapToInt(WorkloadEntry::getSnaps).sum();
        assertEquals(73, allen.getSnapsLastGame());
        assertEquals(40 + 66 + 58 + 73, allen.getSnapsLast4Games());
        assertEquals(lastGame, allen.getSnapsLastGame());
        assertEquals(lastFour, allen.getSnapsLast4Games());
    }

    @Test
    void aNewSeason_replacesLastSeasonsHistory_onThePlayersPage() {
        // Arrange: a full four games last season.
        nflverseHasAllensGames(SEASON, 60, 62, 61, 70);
        job.sync();
        flushAndClear();

        // Act: week 1 of the next season.
        nflverseHasAllensGames(SEASON + 1, 55);
        job.sync();
        flushAndClear();

        // Assert: last season's rows are still stored, but the history starts over.
        assertEquals(List.of(55),
            workloadService.getWorkload(allenId).stream().map(WorkloadEntry::getSnaps).toList());
        assertEquals(4, workloadRepository.findBySeason(SEASON).stream()
            .filter(entry -> entry.getPlayerId().equals(allenId)).count());
        assertEquals(0, allen().getSnapsLast4Games());
    }

    @Test
    void storedHistory_hasTheSameJsonShapeAsBefore() {
        // Arrange
        nflverseHasAllensGames(SEASON, 60, 62);
        job.sync();
        flushAndClear();

        // Act
        String body = JsonMapper.builder().build().writeValueAsString(workloadService.getWorkload(allenId));

        // Assert
        assertEquals("[{\"playerId\":" + allenId + ",\"week\":1,\"snaps\":60},"
            + "{\"playerId\":" + allenId + ",\"week\":2,\"snaps\":62}]", body);
    }
}
