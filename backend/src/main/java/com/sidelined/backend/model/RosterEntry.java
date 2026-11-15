package com.sidelined.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// A player on the user's fantasy roster, the slot they fill, and a free-text note.
// The slot is one of the league format's starting slots (by RosterSlot name, e.g. "RB"),
// BENCH or IR.
@JsonPropertyOrder({"id", "playerId", "slot", "note"})
@Entity
@Table(name = "roster_entries")
public class RosterEntry {

    public static final String BENCH = "BENCH";
    public static final String IR = "IR";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The link to the players table. Its foreign key stops a rostered player from being deleted.
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    // The same player_id column, read-only. It keeps the JSON's "playerId" (in both directions)
    // and lets the roster rules check a player's id without loading the player.
    // Hibernate builds the column from this mapping, so it repeats nullable = false.
    @Column(name = "player_id", nullable = false, insertable = false, updatable = false)
    private Long playerId;

    @Column(nullable = false, length = 16)
    private String slot;

    @Column(columnDefinition = "text")
    private String note;

    // For JPA and for JSON request bodies.
    public RosterEntry() {
    }

    // A new entry, ready to save. The database assigns the id.
    public RosterEntry(Player player, String slot, String note) {
        this.player = player;
        this.playerId = player.getId();
        this.slot = slot;
        this.note = note;
    }

    // A request or test value that names its player by id only. It has no player link,
    // so it can't be saved: RosterEntryService looks the player up and uses the constructor above.
    public RosterEntry(Long id, Long playerId, String slot, String note) {
        this.id = id;
        this.playerId = playerId;
        this.slot = slot;
        this.note = note;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Player getPlayer() { return player; }

    public Long getPlayerId() { return playerId; }
    // Only for JSON request bodies; changing it on a saved entry doesn't move the link.
    public void setPlayerId(Long playerId) { this.playerId = playerId; }

    public String getSlot() { return slot; }
    public void setSlot(String slot) { this.slot = slot; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
