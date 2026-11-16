package com.sidelined.backend.service;

// One row of nflverse's players file, as nflverse writes it: the position is theirs
// ("K", "OLB", "OT"), not yet a Position.
public record NflversePlayer(String gsisId, String name, String position) {
}
