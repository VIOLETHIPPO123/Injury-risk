package com.sidelined.backend.model;

import java.util.List;
import java.util.Optional;

// A league's roster rules. maxRosterSize counts starters and bench; IR spots are on top of it.
public record RosterFormat(
        String id,
        String name,
        List<RosterSlot> slots,
        int benchCount,
        int irCount,
        int maxRosterSize) {

    // The starting slot with this name; empty for BENCH, IR or a name the format doesn't have.
    public Optional<RosterSlot> findSlot(String name) {
        return slots.stream().filter(s -> s.name().equals(name)).findFirst();
    }
}
