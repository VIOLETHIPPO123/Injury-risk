package com.sidelined.backend.model;

import java.util.List;

// A group of starting spots on a roster, e.g. "FLEX": 1 spot that takes an RB, WR or TE.
public record RosterSlot(String name, int count, List<RosterPosition> positions) {
}
