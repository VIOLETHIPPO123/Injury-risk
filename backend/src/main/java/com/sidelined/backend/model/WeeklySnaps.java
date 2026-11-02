package com.sidelined.backend.model;

// One week of a player's season: the snaps they played that week, or null for the team's
// bye week. A game the player sat out is 0 snaps, not null; only a bye has no game.
public record WeeklySnaps(int week, Integer snaps) {

    public static WeeklySnaps game(int week, int snaps) {
        return new WeeklySnaps(week, snaps);
    }

    public static WeeklySnaps bye(int week) {
        return new WeeklySnaps(week, null);
    }
}
