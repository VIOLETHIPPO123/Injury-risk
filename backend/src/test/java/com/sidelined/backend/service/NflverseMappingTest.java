package com.sidelined.backend.service;

import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Position;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class NflverseMappingTest {

    // ---------- Positions ----------

    @Test
    void position_everyDepthChartCode_mapsToOurPosition() {
        // Every pos_abb in nflverse's 2026 depth charts.
        Map<String, Position> expected = Map.ofEntries(
            Map.entry("QB", Position.QB), Map.entry("RB", Position.RB), Map.entry("FB", Position.FB),
            Map.entry("WR", Position.WR), Map.entry("TE", Position.TE),
            Map.entry("LT", Position.LT), Map.entry("LG", Position.LG), Map.entry("C", Position.C),
            Map.entry("RG", Position.RG), Map.entry("RT", Position.RT),
            Map.entry("LDE", Position.DE), Map.entry("RDE", Position.DE),
            Map.entry("LDT", Position.DT), Map.entry("RDT", Position.DT), Map.entry("NT", Position.DT),
            Map.entry("WLB", Position.LB), Map.entry("MLB", Position.LB), Map.entry("SLB", Position.LB),
            Map.entry("LILB", Position.LB), Map.entry("RILB", Position.LB),
            Map.entry("LCB", Position.CB), Map.entry("RCB", Position.CB), Map.entry("NB", Position.CB),
            Map.entry("FS", Position.FS), Map.entry("SS", Position.SS),
            Map.entry("PK", Position.PK), Map.entry("P", Position.P), Map.entry("H", Position.H),
            Map.entry("PR", Position.PR), Map.entry("KR", Position.KR), Map.entry("LS", Position.LS));

        expected.forEach((code, position) ->
            assertEquals(Optional.of(position), NflverseMapping.position(code), code));
    }

    @Test
    void position_playersFileCodes_mapWhereTheyNameOneOfOurPositions() {
        assertEquals(Optional.of(Position.PK), NflverseMapping.position("K"));
        assertEquals(Optional.of(Position.LB), NflverseMapping.position("OLB"));
        assertEquals(Optional.of(Position.LB), NflverseMapping.position("ILB"));
        assertEquals(Optional.of(Position.LB), NflverseMapping.position("LB"));
        assertEquals(Optional.of(Position.DE), NflverseMapping.position("DE"));
        assertEquals(Optional.of(Position.DT), NflverseMapping.position("DT"));
        assertEquals(Optional.of(Position.CB), NflverseMapping.position("CB"));
        assertEquals(Optional.of(Position.RB), NflverseMapping.position("HB"));
    }

    @Test
    void position_codesThatDontSayWhichOfOurPositions_areEmpty() {
        // A tackle could be an LT or an RT, a safety an FS or an SS, and so on.
        for (String code : List.of("OT", "T", "G", "OL", "DL", "DB", "S", "SAF")) {
            assertTrue(NflverseMapping.position(code).isEmpty(), code);
        }
    }

    @Test
    void position_unknownBlankOrMissing_isEmpty() {
        assertTrue(NflverseMapping.position("EDGE").isEmpty());
        assertTrue(NflverseMapping.position("").isEmpty());
        assertTrue(NflverseMapping.position(null).isEmpty());
    }

    @Test
    void position_def_isNeverAPersonsPosition() {
        assertTrue(NflverseMapping.position("DEF").isEmpty());
    }

    @Test
    void position_ignoresCaseAndSpaces() {
        assertEquals(Optional.of(Position.QB), NflverseMapping.position(" qb "));
    }

    // ---------- Teams ----------

    @Test
    void team_ourOwnCodes_mapToThemselves() {
        for (NflTeam team : NflTeam.values()) {
            assertEquals(Optional.of(team), NflverseMapping.team(team.name()));
        }
    }

    @Test
    void team_nflversesOwnSpellings_map() {
        assertEquals(Optional.of(NflTeam.LAR), NflverseMapping.team("LA"));
        assertEquals(Optional.of(NflTeam.WAS), NflverseMapping.team("WSH"));
        assertEquals(Optional.of(NflTeam.JAX), NflverseMapping.team("JAC"));
    }

    @Test
    void team_unknownBlankOrMissing_isEmpty() {
        assertTrue(NflverseMapping.team("XYZ").isEmpty());
        assertTrue(NflverseMapping.team("").isEmpty());
        assertTrue(NflverseMapping.team(null).isEmpty());
    }
}
