package com.sidelined.backend.model;

// One game of a player's season: the week it was played and the player's snaps in it.
// A bye week has no entry; a game the player sat out is 0 snaps.
public record WorkloadEntry(Long playerId, int week, int snaps) {

    public WorkloadEntry {
        if (snaps < 0) {
            throw new IllegalArgumentException("Snaps cannot be negative: " + snaps);
        }
    }
}
