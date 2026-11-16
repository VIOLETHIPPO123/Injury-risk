package com.sidelined.backend.exception;

// nflverse couldn't be reached, or sent something that isn't the file we asked for.
public class NflverseUnavailableException extends RuntimeException {

    public NflverseUnavailableException(String message) {
        super(message);
    }

    public NflverseUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
