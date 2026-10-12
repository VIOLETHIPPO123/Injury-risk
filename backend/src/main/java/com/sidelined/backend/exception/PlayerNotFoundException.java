package com.sidelined.backend.exception;

// Domain exception: says WHAT went wrong, not which HTTP status to send.
public class PlayerNotFoundException extends RuntimeException {
    public PlayerNotFoundException(Long playerId) {
        super("Player not found: " + playerId);
    }
}
