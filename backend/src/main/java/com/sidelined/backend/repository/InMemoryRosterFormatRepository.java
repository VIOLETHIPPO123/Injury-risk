package com.sidelined.backend.repository;

import com.sidelined.backend.model.RosterFormat;
import com.sidelined.backend.model.RosterSlot;
import org.springframework.stereotype.Repository;
import java.util.List;

import static com.sidelined.backend.model.RosterPosition.*;

// The supported formats, as data. A new format is a new entry here, not new code.
@Repository
public class InMemoryRosterFormatRepository
        extends InMemoryReadOnlyRepository<RosterFormat, String>
        implements RosterFormatRepository {

    public static final String ESPN_STANDARD = "espn-standard";
    public static final String YAHOO_STANDARD = "yahoo-standard";
    public static final String SLEEPER_DEFAULT = "sleeper-default";

    private static final List<RosterFormat> FORMATS = List.of(
        new RosterFormat(ESPN_STANDARD, "ESPN Standard", List.of(
            new RosterSlot("QB", 1, List.of(QB)),
            new RosterSlot("RB", 2, List.of(RB)),
            new RosterSlot("WR", 2, List.of(WR)),
            new RosterSlot("TE", 1, List.of(TE)),
            new RosterSlot("K", 1, List.of(K)),
            new RosterSlot("DEF", 1, List.of(DEF)),
            new RosterSlot("FLEX", 1, List.of(RB, WR, TE))
        ), 7, 1, 16),

        new RosterFormat(YAHOO_STANDARD, "Yahoo Standard", List.of(
            new RosterSlot("QB", 1, List.of(QB)),
            new RosterSlot("RB", 2, List.of(RB)),
            new RosterSlot("WR", 2, List.of(WR)),
            new RosterSlot("TE", 1, List.of(TE)),
            new RosterSlot("K", 1, List.of(K)),
            new RosterSlot("DEF", 1, List.of(DEF)),
            new RosterSlot("FLEX", 1, List.of(RB, WR, TE))
        ), 6, 2, 15),

        new RosterFormat(SLEEPER_DEFAULT, "Sleeper Default", List.of(
            new RosterSlot("QB", 1, List.of(QB)),
            new RosterSlot("RB", 2, List.of(RB)),
            new RosterSlot("WR", 2, List.of(WR)),
            new RosterSlot("TE", 1, List.of(TE)),
            new RosterSlot("K", 1, List.of(K)),
            new RosterSlot("DEF", 1, List.of(DEF)),
            new RosterSlot("FLEX", 2, List.of(WR, RB, TE))
        ), 5, 0, 15)
    );

    public InMemoryRosterFormatRepository() {
        super(FORMATS, RosterFormat::id);
    }
}
