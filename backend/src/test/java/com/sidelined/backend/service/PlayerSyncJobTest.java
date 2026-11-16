package com.sidelined.backend.service;

import com.sidelined.backend.exception.NflverseUnavailableException;
import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Player;
import com.sidelined.backend.model.Position;
import com.sidelined.backend.model.Workload;
import com.sidelined.backend.repository.JpaPlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// The download (NflverseClient) and the table are mocked: these check what the job decides to
// save. PlayerSyncPersistenceTest runs it against real tables.
class PlayerSyncJobTest {

    private static final String MAHOMES = "00-0033873";
    private static final String KELCE = "00-0030506";

    private final NflverseClient nflverse = mock(NflverseClient.class);
    private final JpaPlayerRepository playerRepository = mock(JpaPlayerRepository.class);
    private final PlayerSyncJob job = new PlayerSyncJob(nflverse, playerRepository);

    // By default the table already has all 32 defenses and no players.
    @BeforeEach
    void tableHasEveryDefense() {
        when(playerRepository.findAll()).thenReturn(List.of());
        when(playerRepository.findByPosition("DEF")).thenReturn(
            Arrays.stream(NflTeam.values()).map(Player::teamDefense).toList());
    }

    private void nflverseHas(List<NflversePlayer> players, List<DepthChartEntry> chart) {
        when(nflverse.players()).thenReturn(players);
        when(nflverse.latestDepthChart()).thenReturn(chart);
    }

    @SuppressWarnings("unchecked")
    private List<Player> saved() {
        ArgumentCaptor<List<Player>> captor = ArgumentCaptor.forClass(List.class);
        verify(playerRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    // The one player a test saved, when it expects exactly one.
    private Player onlySaved() {
        List<Player> saved = saved();
        assertEquals(1, saved.size(), "saved: " + saved.stream().map(Player::getName).toList());
        return saved.get(0);
    }

    // ---------- Adding and updating ----------

    @Test
    void newPlayer_isAddedWithNflversesIdNameTeamAndPosition() {
        // Arrange
        nflverseHas(
            List.of(new NflversePlayer(MAHOMES, "Patrick Mahomes", "QB")),
            List.of(new DepthChartEntry(MAHOMES, "KC", "QB", 1)));

        // Act
        PlayerSyncJob.Result result = job.sync();

        // Assert
        Player mahomes = onlySaved();
        assertNull(mahomes.getId(), "the database gives a new player its id");
        assertEquals(MAHOMES, mahomes.getGsisId());
        assertEquals("Patrick Mahomes", mahomes.getName());
        assertEquals(NflTeam.KC, mahomes.getTeam());
        assertEquals(Position.QB, mahomes.getPosition());
        assertEquals(0, mahomes.getSnapsLast4Games());
        assertEquals(new PlayerSyncJob.Result(1, 0, 0, 0), result);
    }

    @Test
    void storedPlayer_isUpdatedInPlace_keepingItsIdAndSnaps() {
        // Arrange: Kelce is stored as a KC TE; nflverse now has him as an LV FB under a new name.
        Player stored = new Player(7L, KELCE, "Travis Kelce", NflTeam.KC, Position.TE, new Workload(50, 200));
        when(playerRepository.findAll()).thenReturn(List.of(stored));
        nflverseHas(
            List.of(new NflversePlayer(KELCE, "Travis Kelce Sr.", "TE")),
            List.of(new DepthChartEntry(KELCE, "LV", "FB", 1)));

        // Act
        PlayerSyncJob.Result result = job.sync();

        // Assert
        assertSame(stored, onlySaved(), "the same row, not a new one");
        assertEquals(7L, stored.getId());
        assertEquals(KELCE, stored.getGsisId());
        assertEquals("Travis Kelce Sr.", stored.getName());
        assertEquals(NflTeam.LV, stored.getTeam());
        assertEquals(Position.FB, stored.getPosition());
        assertEquals(50, stored.getSnapsLastGame());
        assertEquals(200, stored.getSnapsLast4Games());
        assertEquals(new PlayerSyncJob.Result(0, 1, 0, 0), result);
    }

    @Test
    void storedPlayer_withNothingChanged_isNotSavedAgain() {
        // Arrange
        Player stored = new Player(7L, KELCE, "Travis Kelce", NflTeam.KC, Position.TE, new Workload(50, 200));
        when(playerRepository.findAll()).thenReturn(List.of(stored));
        nflverseHas(
            List.of(new NflversePlayer(KELCE, "Travis Kelce", "TE")),
            List.of(new DepthChartEntry(KELCE, "KC", "TE", 1)));

        // Act
        PlayerSyncJob.Result result = job.sync();

        // Assert
        assertTrue(saved().isEmpty());
        assertEquals(new PlayerSyncJob.Result(0, 0, 0, 0), result);
    }

    @Test
    void storedPlayerNoLongerOnADepthChart_isLeftAlone() {
        // Arrange
        Player retired = new Player(7L, KELCE, "Travis Kelce", NflTeam.KC, Position.TE, new Workload(50, 200));
        when(playerRepository.findAll()).thenReturn(List.of(retired));
        nflverseHas(
            List.of(new NflversePlayer(KELCE, "Travis Kelce", "TE"), new NflversePlayer(MAHOMES, "Patrick Mahomes", "QB")),
            List.of(new DepthChartEntry(MAHOMES, "KC", "QB", 1)));

        // Act
        job.sync();

        // Assert
        assertEquals("Patrick Mahomes", onlySaved().getName());
        verify(playerRepository, never()).delete(any());
        verify(playerRepository, never()).deleteAll(any());
    }

    @Test
    void nflversesTeamAndPositionCodes_areMappedToOurs() {
        // Arrange: the Rams are "LA" and a left defensive end is "LDE".
        nflverseHas(
            List.of(new NflversePlayer("00-1", "Jared Verse", "LB")),
            List.of(new DepthChartEntry("00-1", "LA", "LDE", 1)));

        // Act
        job.sync();

        // Assert
        Player verse = onlySaved();
        assertEquals(NflTeam.LAR, verse.getTeam());
        assertEquals(Position.DE, verse.getPosition());
    }

    // ---------- Choosing one position ----------

    @Test
    void playerListedTwice_getsTheListingWhereTheyRankHighest() {
        // Arrange: the backup left tackle starts at left guard.
        nflverseHas(
            List.of(new NflversePlayer("00-1", "Swing Lineman", "G")),
            List.of(new DepthChartEntry("00-1", "ARI", "LT", 2), new DepthChartEntry("00-1", "ARI", "LG", 1)));

        // Act
        job.sync();

        // Assert
        assertEquals(Position.LG, onlySaved().getPosition());
    }

    @Test
    void returnerAndHolder_keepTheirRealPosition() {
        // Arrange: the punter holds, and the third receiver is the first kick returner.
        nflverseHas(
            List.of(new NflversePlayer("00-1", "Punter", "P"), new NflversePlayer("00-2", "Receiver", "WR")),
            List.of(
                new DepthChartEntry("00-1", "SEA", "H", 1), new DepthChartEntry("00-1", "SEA", "P", 1),
                new DepthChartEntry("00-2", "SEA", "KR", 1), new DepthChartEntry("00-2", "SEA", "WR", 3)));

        // Act
        job.sync();

        // Assert
        assertEquals(List.of(Position.P, Position.WR), saved().stream().map(Player::getPosition).toList());
    }

    @Test
    void playerWhoOnlyReturnsKicks_isAKickReturner() {
        // Arrange
        nflverseHas(
            List.of(new NflversePlayer("00-1", "Return Specialist", "WR")),
            List.of(new DepthChartEntry("00-1", "SEA", "KR", 1), new DepthChartEntry("00-1", "SEA", "PR", 2)));

        // Act
        job.sync();

        // Assert
        assertEquals(Position.KR, onlySaved().getPosition());
    }

    // ---------- Skipping what can't be stored ----------

    @Test
    void depthChartPositionWeCantMap_fallsBackToThePlayersFile() {
        // Arrange: a depth chart code we've never seen, but the players file says DE.
        nflverseHas(
            List.of(new NflversePlayer("00-1", "Edge Rusher", "DE")),
            List.of(new DepthChartEntry("00-1", "DAL", "EDGE", 1)));

        // Act
        job.sync();

        // Assert
        assertEquals(Position.DE, onlySaved().getPosition());
    }

    @Test
    void positionWeCantMapAtAll_isSkipped_andTheRestAreStillSaved() {
        // Arrange: "EDGE" is unknown and "OT" doesn't say left or right.
        nflverseHas(
            List.of(new NflversePlayer("00-1", "Mystery Tackle", "OT"), new NflversePlayer(MAHOMES, "Patrick Mahomes", "QB")),
            List.of(new DepthChartEntry("00-1", "DAL", "EDGE", 1), new DepthChartEntry(MAHOMES, "KC", "QB", 1)));

        // Act
        PlayerSyncJob.Result result = assertDoesNotThrow(job::sync);

        // Assert
        assertEquals("Patrick Mahomes", onlySaved().getName());
        assertEquals(new PlayerSyncJob.Result(1, 0, 1, 0), result);
    }

    @Test
    void unknownTeam_isSkipped() {
        // Arrange
        nflverseHas(
            List.of(new NflversePlayer("00-1", "London Player", "QB")),
            List.of(new DepthChartEntry("00-1", "LON", "QB", 1)));

        // Act
        PlayerSyncJob.Result result = job.sync();

        // Assert
        assertTrue(saved().isEmpty());
        assertEquals(1, result.skipped());
    }

    @Test
    void depthChartRowsWithNoIdOrAnIdThePlayersFileLacks_areSkipped() {
        // Arrange: without a gsis_id there is nothing to match the player on next week.
        nflverseHas(
            List.of(new NflversePlayer(MAHOMES, "Patrick Mahomes", "QB")),
            List.of(
                new DepthChartEntry("", "KC", "WR", 4),
                new DepthChartEntry("00-9999999", "KC", "WR", 5),
                new DepthChartEntry(MAHOMES, "KC", "QB", 1)));

        // Act
        PlayerSyncJob.Result result = job.sync();

        // Assert
        assertEquals("Patrick Mahomes", onlySaved().getName());
        assertEquals(2, result.skipped());
    }

    // ---------- Team defenses ----------

    @Test
    void emptyTable_getsOneDefensePerTeam() {
        // Arrange
        when(playerRepository.findByPosition("DEF")).thenReturn(List.of());
        nflverseHas(
            List.of(new NflversePlayer(MAHOMES, "Patrick Mahomes", "QB")),
            List.of(new DepthChartEntry(MAHOMES, "KC", "QB", 1)));

        // Act
        PlayerSyncJob.Result result = job.sync();

        // Assert
        List<Player> defenses = saved().stream().filter(p -> p.getPosition() == Position.DEF).toList();
        assertEquals(32, defenses.size());
        assertEquals(32, defenses.stream().map(Player::getTeam).distinct().count());
        Player chiefs = defenses.stream().filter(p -> p.getTeam() == NflTeam.KC).findFirst().orElseThrow();
        assertEquals("Kansas City Chiefs D/ST", chiefs.getName());
        assertNull(chiefs.getGsisId());
        assertEquals(32, result.defensesAdded());
    }

    @Test
    void onlyTheMissingDefenses_areAdded() {
        // Arrange: every defense but Arizona's and Buffalo's is stored.
        when(playerRepository.findByPosition("DEF")).thenReturn(Arrays.stream(NflTeam.values())
            .filter(team -> team != NflTeam.ARI && team != NflTeam.BUF)
            .map(Player::teamDefense).toList());
        nflverseHas(
            List.of(new NflversePlayer(MAHOMES, "Patrick Mahomes", "QB")),
            List.of(new DepthChartEntry(MAHOMES, "KC", "QB", 1)));

        // Act
        job.sync();

        // Assert
        List<NflTeam> added = saved().stream()
            .filter(p -> p.getPosition() == Position.DEF).map(Player::getTeam).toList();
        assertEquals(List.of(NflTeam.ARI, NflTeam.BUF), added);
    }

    // ---------- Failures ----------

    @Test
    void downloadFails_throws_andSavesNothing() {
        // Arrange
        when(nflverse.players()).thenThrow(new NflverseUnavailableException("down"));

        // Act + Assert
        assertThrows(NflverseUnavailableException.class, job::sync);
        verify(playerRepository, never()).saveAll(any());
    }

    @Test
    void emptyDepthChart_throws_andSavesNothing() {
        // Arrange
        nflverseHas(List.of(new NflversePlayer(MAHOMES, "Patrick Mahomes", "QB")), List.of());

        // Act + Assert
        assertThrows(NflverseUnavailableException.class, job::sync);
        verify(playerRepository, never()).saveAll(any());
    }

    @Test
    void weeklyRun_syncs_andAFailureDoesntEscapeTheScheduler() {
        // Arrange: the first run works, the second finds nflverse down.
        when(nflverse.players())
            .thenReturn(List.of(new NflversePlayer(MAHOMES, "Patrick Mahomes", "QB")))
            .thenThrow(new NflverseUnavailableException("down"));
        when(nflverse.latestDepthChart()).thenReturn(List.of(new DepthChartEntry(MAHOMES, "KC", "QB", 1)));

        // Act
        job.runWeekly();
        assertDoesNotThrow(job::runWeekly);

        // Assert
        assertEquals("Patrick Mahomes", onlySaved().getName());
    }
}
