package com.sidelined.backend.controller;

import com.sidelined.backend.exception.PlayerNotFoundException;
import com.sidelined.backend.exception.WatchlistEntryNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

// The ONLY class that decides how errors become HTTP responses.
// Changing the API's error format means changing this file, never the services.
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({PlayerNotFoundException.class, WatchlistEntryNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(RuntimeException ex) {
        return Map.of("error", ex.getMessage());
    }
}
