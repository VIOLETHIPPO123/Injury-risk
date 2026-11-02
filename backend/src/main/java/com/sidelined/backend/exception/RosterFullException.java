package com.sidelined.backend.exception;

public class RosterFullException extends RosterConflictException {
    public RosterFullException(int maxRosterSize) {
        super("Roster is full (" + maxRosterSize + " players plus IR)");
    }
}
