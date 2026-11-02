package com.sidelined.backend.exception;

public class PlayerAlreadyOnRosterException extends RosterConflictException {
    public PlayerAlreadyOnRosterException(Long playerId) {
        super("Player already on the roster: " + playerId);
    }
}
