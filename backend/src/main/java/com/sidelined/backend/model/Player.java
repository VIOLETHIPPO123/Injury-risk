package com.sidelined.backend.model;

public class Player {

    private Long id;
    private String name;
    private NflTeam team;
    private Position position;
    private int snapsLastGame;
    private int snapsLast4Games;

    public Player(Long id, String name, NflTeam team, Position position, int snapsLastGame, int snapsLast4Games) {
        this.id = id;
        this.name = name;
        this.team = team;
        this.position = position;
        this.snapsLastGame = snapsLastGame;
        this.snapsLast4Games = snapsLast4Games;
    }

    public Long getId()           { return id; }
    public String getName()       { return name; }
    public NflTeam getTeam()       { return team; }
    public Position getPosition()     { return position; }
    public int getSnapsLastGame() { return snapsLastGame; }
    public int getSnapsLast4Games() { return snapsLast4Games; }
}
