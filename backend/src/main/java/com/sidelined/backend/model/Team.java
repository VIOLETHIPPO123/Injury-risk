package com.sidelined.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// One of the 32 NFL teams, keyed by its code. TeamDataLoader fills the table at startup.
@Entity
@Table(name = "teams")
public class Team {

    // Stored as the code's text ("ARI"); in JSON it is the same string.
    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 3)
    private NflTeam code;

    @Column(nullable = false)
    private String name;

    // For JPA.
    protected Team() {
    }

    public Team(NflTeam code, String name) {
        this.code = code;
        this.name = name;
    }

    public NflTeam getCode() { return code; }
    public String getName()  { return name; }
}
