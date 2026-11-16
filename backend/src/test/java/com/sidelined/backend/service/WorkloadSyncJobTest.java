package com.sidelined.backend.service;

import com.sidelined.backend.exception.NflverseUnavailableException;
import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Player;
import com.sidelined.backend.model.Position;
import com.sidelined.backend.model.Workload;
import com.sidelined.backend.model.WorkloadEntry;
import com.sidelined.backend.repository.JpaPlayerRepository;
import com.sidelined.backend.repository.JpaWorkloadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// The download (NflverseClient) and the tables are mocked: these check what the job decides to
// save. WorkloadSyncPersistenceTest runs it against real tables.
class WorkloadSyncJobTest {

    private static final int SEASON = 2026;

    private final NflverseClient nflverse = mock(NflverseClient.class);
    private final JpaPlayerRepository playerRepository = mock(JpaPlayerRepository.class);
    private final JpaWorkloadRepository workloadRepository = mock(JpaWorkloadRepository.class);
    private final WorkloadSyncJob job = new WorkloadSyncJob(nflverse, playerRepository, workloadRepository);

    // Two stored players, a stored team defense, and nflverse's players file linking the two
    // players' ids. Their snap columns hold stale numbers the job should replace.
    private final Player mahomes = new Player(1L, "00-0033873", "Patrick Mahomes", NflTeam.KC, Position.QB, new Workload(9, 99));
    private final Player kelce = new Player(2L, "00-0030506", "Travis Kelce", NflTeam.KC, Position.TE, new Workload(9, 99));
    private final Player chiefsDefense = new Player(3L, null, "Kansas City Chiefs D/ST", NflTeam.KC, Position.DEF, new Workload(0, 0));

    @BeforeEach
    void storedPlayersAndIds() {
        when(playerRepository.findAll()).thenReturn(List.of(mahomes, kelce, chiefsDefense));
        when(workloadRepository.findBySeason(SEASON)).thenReturn(List.of());
        when(nflverse.players()).thenReturn(List.of(
            new NflversePlayer("00-0033873", "MahoPa00", "Patrick Mahomes", "QB"),
            new NflversePlayer("00-0030506", "KelcTr00", "Travis Kelce", "TE"),
            // In nflverse's players file, but not in our players table.
            new NflversePlayer("00-0099999", "FolkNi00", "Nick Folk", "K")));
    }

    private static SnapCount mahomes(int week, int snaps) {
        return new SnapCount("MahoPa00", "Patrick Mahomes", SEASON, week, snaps);
    }

    private static SnapCount kelce(int week, int snaps) {
        return new SnapCount("KelcTr00", "Travis Kelce", SEASON, week, snaps);
    }

    private void nflverseHas(SnapCount... snapCounts) {
        when(nflverse.snapCounts()).thenReturn(List.of(snapCounts));
    }

    @SuppressWarnings("unchecked")
    private List<WorkloadEntry> savedEntries() {
        ArgumentCaptor<List<WorkloadEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(workloadRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private List<Player> savedPlayers() {
        ArgumentCaptor<List<Player>> captor = ArgumentCaptor.forClass(List.class);
        verify(playerRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    // ---------- Storing games ----------

    @Test
    void snapCount_isStoredAgainstThePlayerItsPfrIdMapsTo() {
        // Arrange
        nflverseHas(mahomes(1, 69));

        // Act
        WorkloadSyncJob.Result result = job.sync();

        // Assert
        List<WorkloadEntry> saved = savedEntries();
        assertEquals(1, saved.size());
        WorkloadEntry entry = saved.get(0);
        assertSame(mahomes, entry.getPlayer());
        assertEquals(1L, entry.getPlayerId());
        assertEquals(SEASON, entry.getSeason());
        assertEquals(1, entry.getWeek());
        assertEquals(69, entry.getSnaps());
        assertEquals(new WorkloadSyncJob.Result(1, 0, 0), result);
    }

    @Test
    void gameAlreadyStored_isNotAddedAgain() {
        // Arrange: last Tuesday's run stored weeks 1 and 2; this week's file has week 3 too.
        when(workloadRepository.findBySeason(SEASON)).thenReturn(List.of(
            new WorkloadEntry(mahomes, SEASON, 1, 69), new WorkloadEntry(mahomes, SEASON, 2, 80)));
        nflverseHas(mahomes(1, 69), mahomes(2, 80), mahomes(3, 64));

        // Act
        WorkloadSyncJob.Result result = job.sync();

        // Assert
        List<WorkloadEntry> saved = savedEntries();
        assertEquals(List.of(3), saved.stream().map(WorkloadEntry::getWeek).toList());
        assertEquals(new WorkloadSyncJob.Result(1, 0, 0), result);
    }

    @Test
    void runAgainWithTheSameFile_savesNoEntries() {
        // Arrange
        when(workloadRepository.findBySeason(SEASON)).thenReturn(List.of(
            new WorkloadEntry(mahomes, SEASON, 1, 69), new WorkloadEntry(kelce, SEASON, 1, 55)));
        nflverseHas(mahomes(1, 69), kelce(1, 55));

        // Act
        WorkloadSyncJob.Result result = job.sync();

        // Assert
        assertTrue(savedEntries().isEmpty());
        assertEquals(new WorkloadSyncJob.Result(0, 0, 0), result);
    }

    @Test
    void correctedSnapCount_updatesTheStoredRow() {
        // Arrange: nflverse first published 69 snaps for week 1, then corrected it to 71.
        WorkloadEntry stored = new WorkloadEntry(mahomes, SEASON, 1, 69);
        when(workloadRepository.findBySeason(SEASON)).thenReturn(List.of(stored));
        nflverseHas(mahomes(1, 71));

        // Act
        WorkloadSyncJob.Result result = job.sync();

        // Assert
        List<WorkloadEntry> saved = savedEntries();
        assertEquals(1, saved.size());
        assertSame(stored, saved.get(0), "the same row, not a second one for the week");
        assertEquals(71, stored.getSnaps());
        assertEquals(new WorkloadSyncJob.Result(0, 1, 0), result);
    }

    @Test
    void twoRowsForOnePlayerInOneWeek_becomeOneEntry() {
        // Arrange
        nflverseHas(mahomes(1, 40), mahomes(1, 29));

        // Act
        job.sync();

        // Assert: the unique (player, season, week) holds.
        List<WorkloadEntry> saved = savedEntries();
        assertEquals(1, saved.size());
        assertEquals(69, saved.get(0).getSnaps());
    }

    // ---------- Snap counts with no player ----------

    @Test
    void snapCountsThatMatchNoStoredPlayer_areCounted_andTheRestAreStillStored() {
        // Arrange: a PFR id the players file doesn't have, a player who isn't in our table
        // (two games), and a blank id.
        nflverseHas(
            new SnapCount("WhitCo99", "Cody White", SEASON, 1, 30),
            new SnapCount("FolkNi00", "Nick Folk", SEASON, 1, 8),
            new SnapCount("FolkNi00", "Nick Folk", SEASON, 2, 9),
            new SnapCount("", "No Id", SEASON, 1, 12),
            mahomes(1, 69));

        // Act
        WorkloadSyncJob.Result result = assertDoesNotThrow(job::sync);

        // Assert
        assertEquals(List.of("Patrick Mahomes"),
            savedEntries().stream().map(entry -> entry.getPlayer().getName()).toList());
        assertEquals(new WorkloadSyncJob.Result(1, 0, 4), result);
    }

    // ---------- Recalculating the snap columns ----------

    @Test
    void afterARun_eachPlayersSnapColumnsComeFromTheirStoredHistory() {
        // Arrange: weeks 1-4 were stored by earlier runs; week 5 is new. The file lists the
        // games out of order, and Kelce has only played three.
        List<WorkloadEntry> stored = new ArrayList<>();
        int[] earlier = {69, 80, 64, 72};
        for (int week = 1; week <= 4; week++) {
            stored.add(new WorkloadEntry(mahomes, SEASON, week, earlier[week - 1]));
        }
        when(workloadRepository.findBySeason(SEASON)).thenReturn(stored);
        nflverseHas(mahomes(5, 75), mahomes(3, 64), mahomes(1, 69), mahomes(4, 72), mahomes(2, 80),
            kelce(1, 50), kelce(2, 52), kelce(5, 49));

        // Act
        job.sync();

        // Assert: Mahomes' last game is week 5 and his last four are weeks 2-5.
        assertEquals(75, mahomes.getSnapsLastGame());
        assertEquals(80 + 64 + 72 + 75, mahomes.getSnapsLast4Games());
        // Three games is too few for an ACWR, on the chart and so here.
        assertEquals(0, kelce.getSnapsLastGame());
        assertEquals(0, kelce.getSnapsLast4Games());
        assertEquals(List.of(mahomes, kelce), savedPlayers());
    }

    @Test
    void weekThePlayerMissed_isSkipped_notCountedAsZero() {
        // Arrange: Mahomes sat out weeks 3 and 4 (no rows), like a bye.
        nflverseHas(mahomes(1, 69), mahomes(2, 80), mahomes(5, 60), mahomes(6, 71));

        // Act
        job.sync();

        // Assert: his last four games are weeks 1, 2, 5 and 6.
        assertEquals(71, mahomes.getSnapsLastGame());
        assertEquals(69 + 80 + 60 + 71, mahomes.getSnapsLast4Games());
    }

    @Test
    void storedGameTheFileNoLongerLists_stillCounts() {
        // Arrange: the history is what's stored, not what this week's file happens to hold.
        when(workloadRepository.findBySeason(SEASON)).thenReturn(List.of(
            new WorkloadEntry(mahomes, SEASON, 1, 69), new WorkloadEntry(mahomes, SEASON, 2, 80),
            new WorkloadEntry(mahomes, SEASON, 3, 64)));
        nflverseHas(mahomes(4, 72));

        // Act
        job.sync();

        // Assert
        assertEquals(72, mahomes.getSnapsLastGame());
        assertEquals(69 + 80 + 64 + 72, mahomes.getSnapsLast4Games());
    }

    @Test
    void playerWithNoGamesThisSeason_goesToZero_andUnchangedPlayersArentSaved() {
        // Arrange: only Mahomes has played. Kelce's stale numbers are cleared; the defense
        // was already at zero.
        nflverseHas(mahomes(1, 69), mahomes(2, 80), mahomes(3, 64), mahomes(4, 72));

        // Act
        job.sync();

        // Assert
        assertEquals(0, kelce.getSnapsLast4Games());
        assertEquals(List.of(mahomes, kelce), savedPlayers());
    }

    @Test
    void lastSeasonsGames_dontCountTowardThisSeasonsColumns() {
        // Arrange: only this season's stored games are read.
        nflverseHas(mahomes(1, 69));

        // Act
        job.sync();

        // Assert
        verify(workloadRepository).findBySeason(SEASON);
        verify(workloadRepository, never()).findAll();
        assertEquals(0, mahomes.getSnapsLast4Games());
    }

    // ---------- Nothing to do, and failures ----------

    @Test
    void noSnapCountsYet_changesNothing() {
        // Arrange: before week 1 the season's file can be empty.
        nflverseHas();

        // Act
        WorkloadSyncJob.Result result = job.sync();

        // Assert: last season's numbers stay until there is something to replace them.
        assertEquals(new WorkloadSyncJob.Result(0, 0, 0), result);
        verify(workloadRepository, never()).saveAll(any());
        verify(playerRepository, never()).saveAll(any());
        assertEquals(99, mahomes.getSnapsLast4Games());
    }

    @Test
    void downloadFails_throws_andSavesNothing() {
        // Arrange
        when(nflverse.snapCounts()).thenThrow(new NflverseUnavailableException("down"));

        // Act + Assert
        assertThrows(NflverseUnavailableException.class, job::sync);
        verify(workloadRepository, never()).saveAll(any());
        verify(playerRepository, never()).saveAll(any());
    }
}
