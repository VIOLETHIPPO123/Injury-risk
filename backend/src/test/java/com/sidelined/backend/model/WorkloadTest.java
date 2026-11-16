package com.sidelined.backend.model;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorkloadTest {

    @Test
    void validPair_keepsBothValues() {
        // Act
        Workload workload = new Workload(68, 253);

        // Assert
        assertEquals(68, workload.snapsLastGame());
        assertEquals(253, workload.snapsLast4Games());
    }

    @Test
    void edgeCases_areAllowed() {
        // Act + Assert — no snaps at all, and every snap of the last four coming in the last game.
        assertDoesNotThrow(() -> new Workload(0, 0));
        assertDoesNotThrow(() -> new Workload(60, 60));
    }

    @Test
    void negativeSnapsLastGame_isRejected() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new Workload(-1, 200));
    }

    @Test
    void negativeSnapsLast4Games_isRejected() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new Workload(0, -1));
    }

    @Test
    void moreSnapsLastGameThanInTheLastFour_isRejected() {
        // Act + Assert — the last game is one of the last four, so it can't exceed their total.
        assertThrows(IllegalArgumentException.class, () -> new Workload(254, 253));
    }

    // ---------- fromGames: the snap counts for a season of games, oldest first ----------

    @Test
    void fromGames_fourGames_lastGameIsTheNewest_andTheTotalIsAllFour() {
        // Act
        Workload workload = Workload.fromGames(List.of(60, 62, 61, 70));

        // Assert
        assertEquals(new Workload(70, 253), workload);
    }

    @Test
    void fromGames_moreThanFourGames_countsOnlyTheLastFour() {
        // Act: the first three games are too old to count.
        Workload workload = Workload.fromGames(List.of(99, 99, 99, 50, 40, 30, 20));

        // Assert
        assertEquals(new Workload(20, 140), workload);
    }

    @Test
    void fromGames_fewerThanFourGames_isNoDataYet() {
        // The trend chart has no ACWR before a player's fourth game, so the columns say the same.
        assertEquals(new Workload(0, 0), Workload.fromGames(List.of()));
        assertEquals(new Workload(0, 0), Workload.fromGames(List.of(55)));
        assertEquals(new Workload(0, 0), Workload.fromGames(List.of(55, 60, 65)));
    }

    @Test
    void fromGames_aGameWithNoSnaps_stillCountsAsAGame() {
        // Act: active but never on the field in the newest game.
        Workload workload = Workload.fromGames(List.of(30, 30, 30, 0));

        // Assert
        assertEquals(new Workload(0, 90), workload);
    }

    @Test
    void fromGames_givesTheSameAcwrAsTheTrendChartsLastPoint() {
        // Arrange: the chart scores a game as its snaps over the average of the last four games.
        List<Integer> season = List.of(48, 52, 61, 40, 66, 58, 73);
        double chartsLastPoint = 73 / ((40 + 66 + 58 + 73) / 4.0);

        // Act: the player page divides the two columns the same way.
        Workload workload = Workload.fromGames(season);
        double playerPage = workload.snapsLastGame() / (workload.snapsLast4Games() / 4.0);

        // Assert
        assertEquals(chartsLastPoint, playerPage);
    }
}
