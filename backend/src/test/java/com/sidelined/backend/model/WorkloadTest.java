package com.sidelined.backend.model;

import org.junit.jupiter.api.Test;

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
}
