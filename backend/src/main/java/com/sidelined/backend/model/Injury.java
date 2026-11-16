package com.sidelined.backend.model;

// One injury: whose it was, when it started (season and week), what it was, how many games
// it cost, and where it stands now. Worked out from the weekly reports by InjuryHistory.
// type ("Strain") is always null: the reports only give the body part. It stays in the JSON.
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
