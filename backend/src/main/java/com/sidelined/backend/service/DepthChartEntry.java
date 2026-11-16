package com.sidelined.backend.service;

// One row of nflverse's depth chart, as nflverse writes it: the team ("LA") and position
// ("LDE") are theirs, not yet an NflTeam or Position. Rank 1 is the starter. A player can
// have several rows, e.g. WR and KR.
public record DepthChartEntry(String gsisId, String team, String position, int rank) {
}
