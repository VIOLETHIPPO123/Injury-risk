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
import jakarta.persistence.UniqueConstraint;

// One game of a player's season: the week it was played and the player's snaps in it (offense,
// defense and special teams together). A week the player didn't play, a bye included, has no
// entry. The unique constraint is what guarantees one row per player per week.
// The JSON is only the player's id, the week and the snaps, as it was before this was a table.
@JsonPropertyOrder({"playerId", "week", "snaps"})
@Entity
@Table(name = "workload_entries",
    uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "season", "week"}))
public class WorkloadEntry {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    // The same player_id column, read-only: it is the JSON's "playerId" and lets queries filter
    // by player without loading one. Hibernate builds the column from this mapping, so it
    // repeats nullable = false.
    @Column(name = "player_id", nullable = false, insertable = false, updatable = false)
    private Long playerId;

    // The year the season started (the 2026 season ends in February 2027).
    @JsonIgnore
    @Column(nullable = false)
    private int season;

    // nflverse's week number: 1 to 18 in the regular season, then on through the playoffs.
    @Column(nullable = false)
    private int week;

    @Column(nullable = false)
    private int snaps;

    // For JPA.
    protected WorkloadEntry() {
    }

    public WorkloadEntry(Player player, int season, int week, int snaps) {
        this.player = player;
        this.playerId = player.getId();
        this.season = season;
        this.week = week;
        setSnaps(snaps);
    }

    public Long getId()       { return id; }
    public Player getPlayer() { return player; }
    public Long getPlayerId() { return playerId; }
    public int getSeason()    { return season; }
    public int getWeek()      { return week; }
    public int getSnaps()     { return snaps; }

    // nflverse corrects a game's counts now and then; the sync job stores the correction here.
    public void setSnaps(int snaps) {
        if (snaps < 0) {
            throw new IllegalArgumentException("Snaps cannot be negative: " + snaps);
        }
        this.snaps = snaps;
    }
}
