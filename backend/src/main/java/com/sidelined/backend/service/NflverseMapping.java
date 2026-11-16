package com.sidelined.backend.service;

import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Position;
import java.util.Map;
import java.util.Optional;

// Turns nflverse's position and team codes into ours. Anything not listed comes back empty,
// and the sync job logs and skips it.
public final class NflverseMapping {

    // Codes that aren't already a Position name. The depth chart says which side a player
    // lines up on (LDE, RILB); we only keep the position. The players file is vaguer ("K",
    // "OLB"). Its OT, G, OL, DL, DB, S and SAF are left out on purpose: they don't say which
    // of our positions (LT or RT, FS or SS, ...) the player is.
    private static final Map<String, Position> POSITIONS = Map.ofEntries(
        Map.entry("HB", Position.RB),
        Map.entry("LDE", Position.DE),
        Map.entry("RDE", Position.DE),
        Map.entry("LDT", Position.DT),
        Map.entry("RDT", Position.DT),
        Map.entry("NT", Position.DT),
        Map.entry("WLB", Position.LB),
        Map.entry("MLB", Position.LB),
        Map.entry("SLB", Position.LB),
        Map.entry("LILB", Position.LB),
        Map.entry("RILB", Position.LB),
        Map.entry("ILB", Position.LB),
        Map.entry("OLB", Position.LB),
        Map.entry("LCB", Position.CB),
        Map.entry("RCB", Position.CB),
        Map.entry("NB", Position.CB),
        Map.entry("K", Position.PK));

    // nflverse writes the Rams as LA. The others are codes it has used in older files.
    private static final Map<String, NflTeam> TEAMS = Map.of(
        "LA", NflTeam.LAR,
        "WSH", NflTeam.WAS,
        "JAC", NflTeam.JAX);

    private NflverseMapping() {
    }

    public static Optional<Position> position(String nflversePosition) {
        if (nflversePosition == null) {
            return Optional.empty();
        }
        String code = nflversePosition.trim().toUpperCase();
        Position mapped = POSITIONS.get(code);
        if (mapped != null) {
            return Optional.of(mapped);
        }
        // DEF is ours alone: a whole team's defense, never a person in nflverse's files.
        if (code.equals(Position.DEF.name())) {
            return Optional.empty();
        }
        return sameName(Position.class, code);
    }

    public static Optional<NflTeam> team(String nflverseTeam) {
        if (nflverseTeam == null) {
            return Optional.empty();
        }
        String code = nflverseTeam.trim().toUpperCase();
        NflTeam mapped = TEAMS.get(code);
        return mapped != null ? Optional.of(mapped) : sameName(NflTeam.class, code);
    }

    private static <E extends Enum<E>> Optional<E> sameName(Class<E> type, String code) {
        try {
            return Optional.of(Enum.valueOf(type, code));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
