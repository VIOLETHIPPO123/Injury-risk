package com.sidelined.backend.service;

// One row of nflverse's weekly injury report file, as NflverseClient reads it. Blank values are
// null. modifiedAt is when the NFL last changed the row (ISO 8601, so it sorts as text); only
// older files have it.
public record NflverseInjuryReport(
        String gsisId,
        int season,
        int week,
        String primaryInjury,
        String secondaryInjury,
        String gameStatus,
        String practiceStatus,
        String modifiedAt) {
}
