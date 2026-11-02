package com.sidelined.backend.exception;

// Domain exception: says WHAT went wrong, not which HTTP status to send.
public class UnknownRosterFormatException extends RuntimeException {
    public UnknownRosterFormatException(String formatId) {
        super("Unknown roster format: " + formatId);
    }
}
