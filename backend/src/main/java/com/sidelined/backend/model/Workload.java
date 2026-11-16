package com.sidelined.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.List;

// The snap counts that feed the injury-risk (ACWR) score. They only mean something as a pair,
// so they travel together and the rule linking them is checked once, here.
// Embedded in Player, so they are two columns on the players table. The workload sync job
// recalculates them from the player's stored games (fromGames).
@Embeddable
public record Workload(
        @Column(name = "snaps_last_game", nullable = false) int snapsLastGame,
        @Column(name = "snaps_last_4_games", nullable = false) int snapsLast4Games) {

    // How many games the ACWR looks back over.
    public static final int GAMES = 4;

    // The snap counts for a player's season so far: their games' snaps, oldest first, counting
    // only games they played. The last game is the newest one and the four-game total is the
    // last four, that one included. With fewer than four games both are 0, which means "no
    // ACWR yet": the trend chart has no point before a player's fourth game either, so the
    // two always agree.
    public static Workload fromGames(List<Integer> snaps) {
        if (snaps.size() < GAMES) {
            return new Workload(0, 0);
        }
        List<Integer> lastFour = snaps.subList(snaps.size() - GAMES, snaps.size());
        int total = lastFour.stream().mapToInt(Integer::intValue).sum();
        return new Workload(lastFour.get(GAMES - 1), total);
    }

    public Workload {
        if (snapsLastGame < 0 || snapsLast4Games < 0) {
            throw new IllegalArgumentException("Snap counts cannot be negative: "
                + snapsLastGame + ", " + snapsLast4Games);
        }
        // The last game is one of the last four, so it can't exceed their total.
        if (snapsLastGame > snapsLast4Games) {
            throw new IllegalArgumentException("Snaps last game (" + snapsLastGame
                + ") exceed snaps in the last four games (" + snapsLast4Games + ")");
        }
    }
}
