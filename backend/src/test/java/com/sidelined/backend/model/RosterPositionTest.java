package com.sidelined.backend.model;

import org.junit.jupiter.api.Test;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RosterPositionTest {

    @Test
    void placekicker_countsAsK() {
        assertEquals(Optional.of(RosterPosition.K), RosterPosition.of(Position.PK));
    }

    @Test
    void fullback_countsAsRB() {
        assertEquals(Optional.of(RosterPosition.RB), RosterPosition.of(Position.FB));
    }

    @Test
    void skillPositions_mapToThemselves() {
        assertEquals(Optional.of(RosterPosition.QB), RosterPosition.of(Position.QB));
        assertEquals(Optional.of(RosterPosition.RB), RosterPosition.of(Position.RB));
        assertEquals(Optional.of(RosterPosition.WR), RosterPosition.of(Position.WR));
        assertEquals(Optional.of(RosterPosition.TE), RosterPosition.of(Position.TE));
    }

    @Test
    void positionsWithNoFantasySlot_mapToEmpty() {
        for (Position p : new Position[] {Position.LT, Position.C, Position.LB, Position.CB, Position.P, Position.LS}) {
            assertTrue(RosterPosition.of(p).isEmpty(), p + " should have no roster position");
        }
    }
}
