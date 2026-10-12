package com.sidelined.backend.service;

// The one thing the watchlist needs to know about players.
public interface PlayerLookup {
    boolean existsById(Long playerId);
}
