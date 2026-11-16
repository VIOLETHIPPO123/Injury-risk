package com.sidelined.backend.service;

// One row of nflverse's players file, as nflverse writes it: the position is theirs
// ("K", "OLB", "OT"), not yet a Position. pfrId is the player's Pro Football Reference id,
// which the snap counts use; it is empty for players PFR has no page for.
public record NflversePlayer(String gsisId, String pfrId, String name, String position) {
}
