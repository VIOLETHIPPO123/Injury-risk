package com.sidelined.backend.repository;

import com.sidelined.backend.model.RosterFormat;
import com.sidelined.backend.model.RosterPosition;
import com.sidelined.backend.model.RosterSlot;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.sidelined.backend.model.RosterPosition.*;
import static com.sidelined.backend.repository.InMemoryRosterFormatRepository.*;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryRosterFormatRepositoryTest {

    private final InMemoryRosterFormatRepository formatRepository = new InMemoryRosterFormatRepository();

    // Spots per single-position slot, e.g. {QB=1, RB=2, ...}. Flex slots are left out.
    private static Map<RosterPosition, Integer> dedicatedSpots(RosterFormat format) {
        return format.slots().stream()
            .filter(s -> s.positions().size() == 1)
            .collect(Collectors.toMap(s -> s.positions().get(0), RosterSlot::count, Integer::sum));
    }

    private static List<RosterSlot> flexSlots(RosterFormat format) {
        return format.slots().stream().filter(s -> s.positions().size() > 1).toList();
    }

    private RosterFormat format(String id) {
        return formatRepository.findById(id).orElseThrow();
    }

    @Test
    void findAll_returnsTheThreeFormats() {
        List<String> ids = formatRepository.findAll().stream().map(RosterFormat::id).toList();

        assertEquals(List.of(ESPN_STANDARD, YAHOO_STANDARD, SLEEPER_DEFAULT), ids);
    }

    @Test
    void everyFormat_starts1Qb2Rb2Wr1Te1K1Def() {
        Map<RosterPosition, Integer> expected = Map.of(QB, 1, RB, 2, WR, 2, TE, 1, K, 1, DEF, 1);

        for (RosterFormat format : formatRepository.findAll()) {
            assertEquals(expected, dedicatedSpots(format), format.name());
        }
    }

    @Test
    void everyFormat_startersPlusBenchEqualsMaxRosterSize() {
        for (RosterFormat format : formatRepository.findAll()) {
            int starters = format.slots().stream().mapToInt(RosterSlot::count).sum();
            assertEquals(format.maxRosterSize(), starters + format.benchCount(), format.name());
        }
    }

    @Test
    void espnStandard_has1Flex_7Bench_1Ir_max16() {
        RosterFormat espn = format(ESPN_STANDARD);

        List<RosterSlot> flex = flexSlots(espn);
        assertEquals(1, flex.size());
        assertEquals("FLEX", flex.get(0).name());
        assertEquals(1, flex.get(0).count());
        assertEquals(List.of(RB, WR, TE), flex.get(0).positions());
        assertEquals(7, espn.benchCount());
        assertEquals(1, espn.irCount());
        assertEquals(16, espn.maxRosterSize());
    }

    @Test
    void yahooStandard_has1Flex_6Bench_2Ir_max15() {
        RosterFormat yahoo = format(YAHOO_STANDARD);

        List<RosterSlot> flex = flexSlots(yahoo);
        assertEquals(1, flex.size());
        assertEquals("FLEX", flex.get(0).name());
        assertEquals(1, flex.get(0).count());
        assertEquals(List.of(RB, WR, TE), flex.get(0).positions());
        assertEquals(6, yahoo.benchCount());
        assertEquals(2, yahoo.irCount());
        assertEquals(15, yahoo.maxRosterSize());
    }

    @Test
    void sleeperDefault_has2Flex_5Bench_0Ir_max15() {
        RosterFormat sleeper = format(SLEEPER_DEFAULT);

        List<RosterSlot> flex = flexSlots(sleeper);
        assertEquals(1, flex.size());
        assertEquals("FLEX", flex.get(0).name());
        assertEquals(2, flex.get(0).count());
        assertEquals(List.of(WR, RB, TE), flex.get(0).positions());
        assertEquals(5, sleeper.benchCount());
        assertEquals(0, sleeper.irCount());
        assertEquals(15, sleeper.maxRosterSize());
    }

    @Test
    void findById_unknownFormat_returnsEmpty() {
        assertTrue(formatRepository.findById("nope").isEmpty());
    }
}
