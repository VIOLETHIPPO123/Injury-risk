package com.sidelined.backend;

import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Player;
import com.sidelined.backend.model.Position;
import com.sidelined.backend.repository.JpaPlayerRepository;
import com.sidelined.backend.service.DepthChartEntry;
import com.sidelined.backend.service.NflverseClient;
import com.sidelined.backend.service.NflversePlayer;
import com.sidelined.backend.service.PlayerSyncJob;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

// The sync job against real tables (the in-memory H2 database of the "test" profile), with the
// download mocked. Each test runs in a transaction that is rolled back afterwards.
// flushAndClear() writes everything to the tables and forgets it, so the next read comes from
// the database, as it would the following Tuesday.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PlayerSyncPersistenceTest {

    // Ids no other test uses, since tests share the database.
    private static final String ALLEN = "00-SYNC-01";
    private static final String COOK = "00-SYNC-02";
    private static final String NEWCOMER = "00-SYNC-03";

    @Autowired private PlayerSyncJob job;
    @Autowired private JpaPlayerRepository playerRepository;
    @Autowired private EntityManager entityManager;

    @MockitoBean private NflverseClient nflverse;

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private void nflverseHas(List<NflversePlayer> players, List<DepthChartEntry> chart) {
        when(nflverse.players()).thenReturn(players);
        when(nflverse.latestDepthChart()).thenReturn(chart);
    }

    private void nflverseHasAllenAndCook() {
        nflverseHas(
            List.of(new NflversePlayer(ALLEN, "", "Josh Allen", "QB"), new NflversePlayer(COOK, "", "James Cook", "RB")),
            List.of(new DepthChartEntry(ALLEN, "BUF", "QB", 1), new DepthChartEntry(COOK, "BUF", "RB", 1)));
    }

    @Test
    void runningTwice_addsNoDuplicates_andKeepsEveryId() {
        // Arrange
        nflverseHasAllenAndCook();
        long before = playerRepository.count();

        // Act
        job.sync();
        flushAndClear();
        long afterFirstRun = playerRepository.count();
        Long allensId = playerRepository.findByGsisId(ALLEN).orElseThrow().getId();
        Long cooksId = playerRepository.findByGsisId(COOK).orElseThrow().getId();

        PlayerSyncJob.Result second = job.sync();
        flushAndClear();

        // Assert: two players and 32 defenses the first time, nothing the second.
        assertEquals(before + 2 + 32, afterFirstRun);
        assertEquals(afterFirstRun, playerRepository.count());
        assertEquals(new PlayerSyncJob.Result(0, 0, 0, 0), second);
        assertEquals(allensId, playerRepository.findByGsisId(ALLEN).orElseThrow().getId());
        assertEquals(cooksId, playerRepository.findByGsisId(COOK).orElseThrow().getId());
    }

    @Test
    void aLaterRun_updatesThePlayerInItsRow_andAddsNewcomers() {
        // Arrange
        nflverseHasAllenAndCook();
        job.sync();
        flushAndClear();
        Long cooksId = playerRepository.findByGsisId(COOK).orElseThrow().getId();
        long afterFirstRun = playerRepository.count();

        // Act: next week Cook has been traded to Miami and a rookie has appeared.
        nflverseHas(
            List.of(new NflversePlayer(ALLEN, "", "Josh Allen", "QB"), new NflversePlayer(COOK, "", "James Cook III", "RB"),
                new NflversePlayer(NEWCOMER, "", "New Comer", "K")),
            List.of(new DepthChartEntry(ALLEN, "BUF", "QB", 1), new DepthChartEntry(COOK, "MIA", "RB", 2),
                new DepthChartEntry(NEWCOMER, "BUF", "PK", 1)));
        PlayerSyncJob.Result result = job.sync();
        flushAndClear();

        // Assert
        assertEquals(new PlayerSyncJob.Result(1, 1, 0, 0), result);
        assertEquals(afterFirstRun + 1, playerRepository.count());
        Player cook = playerRepository.findByGsisId(COOK).orElseThrow();
        assertEquals(cooksId, cook.getId());
        assertEquals("James Cook III", cook.getName());
        assertEquals(NflTeam.MIA, cook.getTeam());
        assertEquals(Position.PK, playerRepository.findByGsisId(NEWCOMER).orElseThrow().getPosition());
    }

    @Test
    void thereAreExactly32DefRows_oneForEachTeam_howeverOftenItRuns() {
        // Arrange
        nflverseHasAllenAndCook();

        // Act
        job.sync();
        flushAndClear();
        job.sync();
        flushAndClear();

        // Assert
        List<Player> defenses = playerRepository.findByPosition("DEF");
        assertEquals(32, defenses.size());
        assertEquals(32, defenses.stream().map(Player::getTeam).distinct().count());
        assertTrue(defenses.stream().allMatch(defense -> defense.getGsisId() == null));
    }

    @Test
    void syncedPlayersAndDefenses_haveTheSameJsonShapeAsBefore() {
        // Arrange
        nflverseHasAllenAndCook();
        job.sync();
        flushAndClear();
        JsonMapper json = JsonMapper.builder().build();

        // Act
        Player allen = playerRepository.findByGsisId(ALLEN).orElseThrow();
        Player billsDefense = playerRepository.findByPosition("DEF").stream()
            .filter(defense -> defense.getTeam() == NflTeam.BUF).findFirst().orElseThrow();

        // Assert
        assertEquals("{\"id\":" + allen.getId() + ",\"name\":\"Josh Allen\",\"team\":\"BUF\",\"position\":\"QB\","
            + "\"snapsLastGame\":0,\"snapsLast4Games\":0}", json.writeValueAsString(allen));
        assertEquals("{\"id\":" + billsDefense.getId() + ",\"name\":\"Buffalo Bills D/ST\",\"team\":\"BUF\","
            + "\"position\":\"DEF\",\"snapsLastGame\":0,\"snapsLast4Games\":0}", json.writeValueAsString(billsDefense));
    }
}
