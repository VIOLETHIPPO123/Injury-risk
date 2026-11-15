package com.sidelined.backend.model;

import java.util.List;
import java.util.Optional;

import static com.sidelined.backend.model.RosterPosition.*;

// The supported formats, as data. RosterDataLoader puts them in the roster_formats table.
// A new format is a new entry here, not new code.
public final class StandardRosterFormats {

    public static final String ESPN_STANDARD = "espn-standard";
    public static final String YAHOO_STANDARD = "yahoo-standard";
    public static final String SLEEPER_DEFAULT = "sleeper-default";

    private StandardRosterFormats() {
    }

    // Fresh objects on every call, so nothing shares an instance JPA has saved.
    public static List<RosterFormat> all() {
        return List.of(
            new RosterFormat(ESPN_STANDARD, "ESPN Standard", List.of(
                new RosterSlot("QB", 1, List.of(QB)),
                new RosterSlot("RB", 2, List.of(RB)),
                new RosterSlot("WR", 2, List.of(WR)),
                new RosterSlot("TE", 1, List.of(TE)),
                new RosterSlot("K", 1, List.of(K)),
                new RosterSlot("DEF", 1, List.of(DEF)),
                new RosterSlot("FLEX", 1, List.of(RB, WR, TE))
            ), 7, 1, 16, 1),

            new RosterFormat(YAHOO_STANDARD, "Yahoo Standard", List.of(
                new RosterSlot("QB", 1, List.of(QB)),
                new RosterSlot("RB", 2, List.of(RB)),
                new RosterSlot("WR", 2, List.of(WR)),
                new RosterSlot("TE", 1, List.of(TE)),
                new RosterSlot("K", 1, List.of(K)),
                new RosterSlot("DEF", 1, List.of(DEF)),
                new RosterSlot("FLEX", 1, List.of(RB, WR, TE))
            ), 6, 2, 15, 2),

            new RosterFormat(SLEEPER_DEFAULT, "Sleeper Default", List.of(
                new RosterSlot("QB", 1, List.of(QB)),
                new RosterSlot("RB", 2, List.of(RB)),
                new RosterSlot("WR", 2, List.of(WR)),
                new RosterSlot("TE", 1, List.of(TE)),
                new RosterSlot("K", 1, List.of(K)),
                new RosterSlot("DEF", 1, List.of(DEF)),
                new RosterSlot("FLEX", 2, List.of(WR, RB, TE))
            ), 5, 0, 15, 3)
        );
    }

    public static Optional<RosterFormat> byId(String id) {
        return all().stream().filter(f -> f.id().equals(id)).findFirst();
    }
}
