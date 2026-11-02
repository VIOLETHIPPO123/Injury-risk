package com.sidelined.backend.model;

import java.util.Optional;

// The positions a fantasy roster slot can hold. Narrower than Position: linemen,
// punters and return men never go on a fantasy roster, and DEF is a whole team.
public enum RosterPosition {
    QB,
    RB,
    WR,
    TE,
    K,
    DEF;

    // The fantasy position a player counts as. A placekicker (PK) is a K and a fullback (FB)
    // is an RB; any other position has no fantasy position and comes back empty.
    public static Optional<RosterPosition> of(Position position) {
        return switch (position) {
            case QB -> Optional.of(QB);
            case RB, FB -> Optional.of(RB);
            case WR -> Optional.of(WR);
            case TE -> Optional.of(TE);
            case PK -> Optional.of(K);
            default -> Optional.empty();
        };
    }
}
