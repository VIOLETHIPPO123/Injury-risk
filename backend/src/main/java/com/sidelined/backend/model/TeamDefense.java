package com.sidelined.backend.model;

// A team defense, rostered as one DEF entry. Its id shares a space with player ids
// so a roster can hold both, which is why the two must never collide.
public record TeamDefense(Long id, NflTeam team, String name) {

    public RosterPosition position() { return RosterPosition.DEF; }
}
