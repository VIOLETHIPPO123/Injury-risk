package com.sidelined.backend.exception;

public class SlotNotAllowedException extends RosterConflictException {
    public SlotNotAllowedException(String message) {
        super(message);
    }
}
