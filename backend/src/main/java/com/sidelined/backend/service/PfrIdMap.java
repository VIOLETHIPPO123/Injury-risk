package com.sidelined.backend.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Turns the player ids in nflverse's snap counts (Pro Football Reference's, like "MahoPa00")
// into the gsis_id the players table is keyed on. nflverse's players file lists both ids for
// each player, and that is the only place the link exists.
public final class PfrIdMap {

    private final Map<String, String> gsisIdByPfrId = new HashMap<>();

    // Players with either id missing can't be linked and are left out. If nflverse ever lists
    // one PFR id twice, the first row wins.
    public PfrIdMap(List<NflversePlayer> players) {
        for (NflversePlayer player : players) {
            if (!isBlank(player.pfrId()) && !isBlank(player.gsisId())) {
                gsisIdByPfrId.putIfAbsent(player.pfrId().trim(), player.gsisId().trim());
            }
        }
    }

    public Optional<String> gsisId(String pfrId) {
        return isBlank(pfrId) ? Optional.empty() : Optional.ofNullable(gsisIdByPfrId.get(pfrId.trim()));
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
