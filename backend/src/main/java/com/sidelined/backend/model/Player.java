package com.sidelined.backend.model;

public class Player {

    private Long id;
    private String name;
    private String team;
    private int snapsLastGame;
    private int snapsLast4Games;

    public Player(Long id, String name, String team, int snapsLastGame, int snapsLast4Games) {
        this.id = id;
        this.name = name;
        this.team = team;
        this.snapsLastGame = snapsLastGame;
        this.snapsLast4Games = snapsLast4Games;
    }

    public Long getId()           { return id; }
    public String getName()       { return name; }
    public String getTeam()       { return team; }
    public int getSnapsLastGame() { return snapsLastGame; }
    public int getSnapsLast4Games() { return snapsLast4Games; }
}
