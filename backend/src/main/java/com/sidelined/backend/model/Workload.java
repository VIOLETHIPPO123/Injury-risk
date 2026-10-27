package com.sidelined.backend.model;

// The snap counts that feed the injury-risk (ACWR) score. They only mean something as a pair,
// so they travel together and the rule linking them is checked once, here.
public record Workload(int snapsLastGame, int snapsLast4Games) {

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
