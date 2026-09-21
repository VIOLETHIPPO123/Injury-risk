package com.sidelined.backend.model;

// A player the user is keeping an eye on. Kept general (not fantasy-specific) so it works for
// both fantasy managers and sports bettors; the free-text note carries any context.
public class WatchlistEntry {

    private Long id;
    private Long playerId;
    private String note;

    public WatchlistEntry() {
    }

    public WatchlistEntry(Long id, Long playerId, String note) {
        this.id = id;
        this.playerId = playerId;
        this.note = note;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
