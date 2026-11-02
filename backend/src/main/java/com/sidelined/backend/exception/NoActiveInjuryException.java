package com.sidelined.backend.exception;

public class NoActiveInjuryException extends RosterConflictException {
    public NoActiveInjuryException(Long playerId) {
        super("Only a player with an Active injury can go on IR: " + playerId);
    }
}
