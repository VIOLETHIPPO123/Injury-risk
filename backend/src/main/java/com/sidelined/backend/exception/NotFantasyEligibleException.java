package com.sidelined.backend.exception;

// A lineman, an individual defender, a punter or a return man: no roster slot takes them.
public class NotFantasyEligibleException extends RuntimeException {
    public NotFantasyEligibleException() {
        super("Not a fantasy-eligible position.");
    }
}
