package com.sidelined.backend.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

// The snap getters have no field of their own, so Jackson would list them last and swap their
// order. Pinning the order keeps GET /players byte-for-byte what it was before Workload existed.
@JsonPropertyOrder({"id", "name", "team", "position", "snapsLastGame", "snapsLast4Games"})
public class Player {

    private Long id;
    private String name;
    private NflTeam team;
    private Position position;
    private Workload workload;

    public Player(Long id, String name, NflTeam team, Position position, Workload workload) {
        this.id = id;
        this.name = name;
        this.team = team;
        this.position = position;
        this.workload = workload;
    }

    public Long getId()           { return id; }
    public String getName()       { return name; }
    public NflTeam getTeam()       { return team; }
    public Position getPosition()     { return position; }
    // Read through to the workload so the JSON keeps its flat snapsLastGame/snapsLast4Games fields.
    public int getSnapsLastGame() { return workload.snapsLastGame(); }
    public int getSnapsLast4Games() { return workload.snapsLast4Games(); }
}
