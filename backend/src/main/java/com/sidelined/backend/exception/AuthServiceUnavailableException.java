package com.sidelined.backend.exception;

// Supabase Auth couldn't be reached, so sign-up or sign-in couldn't even be attempted.
public class AuthServiceUnavailableException extends RuntimeException {
    public AuthServiceUnavailableException(Throwable cause) {
        super("Sign-in service unavailable", cause);
    }
}
