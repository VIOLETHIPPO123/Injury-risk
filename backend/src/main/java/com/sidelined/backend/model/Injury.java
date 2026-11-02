package com.sidelined.backend.model;

// One past injury: when it happened (season and week), what it was, how many games it cost,
// and where it stands now (e.g. "Recovered", "Questionable").
public record Injury(int season, int week, String bodyPart, String type, int gamesMissed, String status) {
}
