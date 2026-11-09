package com.sidelined.backend.controller;

import com.sidelined.backend.exception.AuthServiceUnavailableException;
import com.sidelined.backend.exception.NotAdminException;
import com.sidelined.backend.exception.NotFantasyEligibleException;
import com.sidelined.backend.exception.PlayerNotFoundException;
import com.sidelined.backend.exception.RosterConflictException;
import com.sidelined.backend.exception.RosterEntryNotFoundException;
import com.sidelined.backend.exception.UnknownRosterFormatException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

// The ONLY class that decides how errors become HTTP responses.
// Changing the API's error format means changing this file, never the services.
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({PlayerNotFoundException.class, RosterEntryNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(RuntimeException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler({UnknownRosterFormatException.class, NotFantasyEligibleException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(RuntimeException ex) {
        return Map.of("error", ex.getMessage());
    }

    // Signed in, but not allowed: 403, not 401 (which means "who are you?").
    @ExceptionHandler(NotAdminException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> handleForbidden(RuntimeException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(AuthServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Map<String, String> handleAuthServiceUnavailable(RuntimeException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(RosterConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleConflict(RuntimeException ex) {
        return Map.of("error", ex.getMessage());
    }
}
