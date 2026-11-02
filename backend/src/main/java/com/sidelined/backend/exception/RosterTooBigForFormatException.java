package com.sidelined.backend.exception;

public class RosterTooBigForFormatException extends RosterConflictException {
    public RosterTooBigForFormatException(String formatName, int maxRosterSize, int rosterSize) {
        super(formatName + " holds " + maxRosterSize + " players; the roster has " + rosterSize);
    }
}
