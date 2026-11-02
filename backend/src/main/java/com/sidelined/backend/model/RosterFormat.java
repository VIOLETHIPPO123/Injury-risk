package com.sidelined.backend.model;

import java.util.List;

// A league's roster rules. maxRosterSize counts starters and bench; IR spots are on top of it.
public record RosterFormat(
        String id,
        String name,
        List<RosterSlot> slots,
        int benchCount,
        int irCount,
        int maxRosterSize) {
}
