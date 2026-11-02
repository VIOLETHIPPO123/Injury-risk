package com.sidelined.backend.exception;

// Domain exception: says WHAT went wrong, not which HTTP status to send.
public class RosterEntryNotFoundException extends RuntimeException {
    public RosterEntryNotFoundException(Long entryId) {
        super("Roster entry not found: " + entryId);
    }
}
