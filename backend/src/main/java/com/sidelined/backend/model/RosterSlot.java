package com.sidelined.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import java.util.List;

// A group of starting spots on a roster, e.g. "FLEX": 1 spot that takes an RB, WR or TE.
// Stored as one row of roster_format_slots; the positions are one text column ("RB,WR,TE").
@Embeddable
public record RosterSlot(
        @Column(nullable = false, length = 16) String name,
        // "count" is an SQL function name, so the column is "spots".
        @Column(name = "spots", nullable = false) int count,
        @Convert(converter = RosterPositionsConverter.class)
        @Column(nullable = false, length = 64) List<RosterPosition> positions) {
}
