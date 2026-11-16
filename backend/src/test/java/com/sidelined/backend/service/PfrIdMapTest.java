package com.sidelined.backend.service;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PfrIdMapTest {

    private final PfrIdMap ids = new PfrIdMap(List.of(
        new NflversePlayer("00-0033873", "MahoPa00", "Patrick Mahomes", "QB"),
        new NflversePlayer("00-0030506", "KelcTr00", "Travis Kelce", "TE"),
        // No Pro Football Reference page, and (an old player) no gsis_id.
        new NflversePlayer("00-0040000", "", "Undrafted Rookie", "WR"),
        new NflversePlayer("", "OldTim00", "Old Timer", "QB")));

    @Test
    void pfrId_mapsToThatPlayersGsisId() {
        assertEquals(Optional.of("00-0033873"), ids.gsisId("MahoPa00"));
        assertEquals(Optional.of("00-0030506"), ids.gsisId("KelcTr00"));
    }

    @Test
    void pfrIdThePlayersFileDoesntHave_isEmpty() {
        assertTrue(ids.gsisId("NobodY00").isEmpty());
    }

    @Test
    void blankOrMissingPfrId_isEmpty_evenThoughSomePlayersHaveNone() {
        // The rookie's blank PFR id must not make every blank snap-count row his.
        assertTrue(ids.gsisId("").isEmpty());
        assertTrue(ids.gsisId("  ").isEmpty());
        assertTrue(ids.gsisId(null).isEmpty());
    }

    @Test
    void playerWithNoGsisId_cantBeLinked() {
        assertTrue(ids.gsisId("OldTim00").isEmpty());
    }

    @Test
    void ids_areMatchedExactly_apartFromSpacesAroundThem() {
        // PFR ids differ only by case or digits for some players (two Josh Allens).
        PfrIdMap allens = new PfrIdMap(List.of(
            new NflversePlayer("00-0034857", "AlleJo02", "Josh Allen", "QB"),
            new NflversePlayer("00-0035639", "AlleJo03", "Josh Hines-Allen", "DE")));

        assertEquals(Optional.of("00-0034857"), allens.gsisId(" AlleJo02 "));
        assertEquals(Optional.of("00-0035639"), allens.gsisId("AlleJo03"));
        assertTrue(allens.gsisId("allejo02").isEmpty());
    }

    @Test
    void pfrIdListedTwice_keepsTheFirst() {
        PfrIdMap twice = new PfrIdMap(List.of(
            new NflversePlayer("00-1", "SameId00", "First", "QB"),
            new NflversePlayer("00-2", "SameId00", "Second", "QB")));

        assertEquals(Optional.of("00-1"), twice.gsisId("SameId00"));
    }
}
