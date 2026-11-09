package com.sidelined.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// The snap getters have no field of their own, so Jackson would list them last and swap their
// order. Pinning the order keeps GET /players byte-for-byte what it was before Workload existed.
@JsonPropertyOrder({"id", "name", "team", "position", "snapsLastGame", "snapsLast4Games"})
@Entity
@Table(name = "players")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // nflverse's player id; the sync job (DEV-102) matches on it. Optional because team
    // defenses are stored as players and have none. Postgres allows many NULLs in a unique column.
    @Column(name = "gsis_id", unique = true)
    private String gsisId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private NflTeam team;

    // Stored as plain text ("QB") instead of @Enumerated: Hibernate adds a database check listing
    // today's positions for any enum field, and ddl-auto=update never widens it, so a new
    // position (DEF in DEV-102) would be rejected. In Java it is still a Position.
    @Column(nullable = false, length = 8)
    private String position;

    @Embedded
    private Workload workload;

    // For JPA.
    protected Player() {
    }

    public Player(Long id, String name, NflTeam team, Position position, Workload workload) {
        this(id, null, name, team, position, workload);
    }

    public Player(Long id, String gsisId, String name, NflTeam team, Position position, Workload workload) {
        this.id = id;
        this.gsisId = gsisId;
        this.name = name;
        this.team = team;
        this.position = position.name();
        this.workload = workload;
    }

    public Long getId()           { return id; }
    @JsonIgnore
    public String getGsisId()     { return gsisId; }
    public String getName()       { return name; }
    public NflTeam getTeam()       { return team; }
    public Position getPosition()     { return Position.valueOf(position); }
    // Read through to the workload so the JSON keeps its flat snapsLastGame/snapsLast4Games fields.
    public int getSnapsLastGame() { return workload.snapsLastGame(); }
    public int getSnapsLast4Games() { return workload.snapsLast4Games(); }
}
