package com.sidelined.backend.model;

// One injury: whose it was, when it happened (season and week), what it was, how many games
// it cost, and where it stands now.
public record Injury(
        Long id,
        Long playerId,
        int season,
        int week,
        String bodyPart,
        String type,
        int gamesMissed,
        InjuryStatus status) {
}
