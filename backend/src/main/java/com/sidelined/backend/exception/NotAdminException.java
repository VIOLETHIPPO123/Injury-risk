package com.sidelined.backend.exception;

// Domain exception: says WHAT went wrong, not which HTTP status to send.
public class NotAdminException extends RuntimeException {
    public NotAdminException() {
        super("Admins only");
    }
}
