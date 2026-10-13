package com.sidelined.backend.exception;

// Domain exception: says WHAT went wrong, not which HTTP status to send.
public class WatchlistEntryNotFoundException extends RuntimeException {
    public WatchlistEntryNotFoundException(Long entryId) {
        super("Watchlist entry not found: " + entryId);
    }
}
