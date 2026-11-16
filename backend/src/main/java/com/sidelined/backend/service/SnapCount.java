package com.sidelined.backend.service;

// One row of nflverse's snap counts file: one player in one game. The player is named by
// Pro Football Reference's id, not the gsis_id the players table uses (see PfrIdMap). Snaps
// are offense, defense and special teams added together.
public record SnapCount(String pfrId, String playerName, int season, int week, int snaps) {
}
