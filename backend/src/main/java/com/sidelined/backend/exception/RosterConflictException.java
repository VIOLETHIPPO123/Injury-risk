package com.sidelined.backend.exception;

// A request that clashes with the roster as it stands (full, already rostered, slot taken...).
// Every subclass becomes a 409 in ApiExceptionHandler, so a new rule never touches the handler.
public abstract class RosterConflictException extends RuntimeException {
    protected RosterConflictException(String message) {
        super(message);
    }
}
