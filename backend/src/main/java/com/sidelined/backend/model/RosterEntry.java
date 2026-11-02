package com.sidelined.backend.model;

// A player on the user's fantasy roster, the slot they fill, and a free-text note.
// The slot is one of the league format's starting slots (by RosterSlot name, e.g. "RB"),
// BENCH or IR.
public class RosterEntry {

    public static final String BENCH = "BENCH";
    public static final String IR = "IR";

    private Long id;
    private Long playerId;
    private String slot;
    private String note;

    public RosterEntry() {
    }

    public RosterEntry(Long id, Long playerId, String slot, String note) {
        this.id = id;
        this.playerId = playerId;
        this.slot = slot;
        this.note = note;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }

    public String getSlot() { return slot; }
    public void setSlot(String slot) { this.slot = slot; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
