package com.sidelined.backend.exception;

public class SlotFullException extends RosterConflictException {
    public SlotFullException(String slot) {
        super("No open spot in " + slot);
    }
}
