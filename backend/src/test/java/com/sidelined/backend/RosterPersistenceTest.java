package com.sidelined.backend;

import com.sidelined.backend.config.RosterDataLoader;
import com.sidelined.backend.exception.PlayerAlreadyOnRosterException;
import com.sidelined.backend.exception.SlotNotAllowedException;
import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Player;
import com.sidelined.backend.model.Position;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.model.RosterFormat;
import com.sidelined.backend.model.RosterPosition;
import com.sidelined.backend.model.RosterSettings;
import com.sidelined.backend.model.StandardRosterFormats;
import com.sidelined.backend.model.Workload;
import com.sidelined.backend.repository.JpaPlayerRepository;
import com.sidelined.backend.repository.JpaRosterEntryRepository;
import com.sidelined.backend.repository.JpaRosterFormatRepository;
import com.sidelined.backend.repository.JpaRosterSettingsRepository;
import com.sidelined.backend.service.RosterEntryService;
import com.sidelined.backend.service.RosterService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;

import static com.sidelined.backend.model.StandardRosterFormats.*;
import static org.junit.jupiter.api.Assertions.*;

// The roster against real tables (the in-memory H2 database of the "test" profile), with every
// rule Spring wires in. Each test runs in a transaction that is rolled back afterwards.
// flushAndClear() writes everything to the tables and forgets it, so the next read comes from
// the database, as it would after a restart.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RosterPersistenceTest {

    @Autowired private RosterEntryService rosterEntryService;
    @Autowired private RosterService rosterService;
    @Autowired private RosterDataLoader rosterDataLoader;
    @Autowired private JpaPlayerRepository playerRepository;
    @Autowired private JpaRosterEntryRepository rosterEntryRepository;
    @Autowired private JpaRosterFormatRepository formatRepository;
    @Autowired private JpaRosterSettingsRepository settingsRepository;
    @Autowired private EntityManager entityManager;

    private final JsonMapper json = JsonMapper.builder().build();

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private Player savePlayer(String name, Position position) {
        return playerRepository.save(new Player(null, name, NflTeam.BUF, position, new Workload(60, 240)));
    }

    // ---------- Formats ----------

    @Test
    void formatsTable_hasTheThreeFormats_andLoadingAgainAddsNone() {
        assertEquals(3, formatRepository.count());

        // A restart runs the loader again.
        rosterDataLoader.run(null);
        flushAndClear();

        assertEquals(3, formatRepository.count());
    }

    @Test
    void formatsTable_readsBackTheSlotsInOrder() {
        RosterFormat sleeper = formatRepository.findById(SLEEPER_DEFAULT).orElseThrow();

        assertEquals(List.of("QB", "RB", "WR", "TE", "K", "DEF", "FLEX"),
            sleeper.slots().stream().map(s -> s.name()).toList());
        assertEquals(List.of(RosterPosition.WR, RosterPosition.RB, RosterPosition.TE),
            sleeper.findSlot("FLEX").orElseThrow().positions());
        assertEquals(2, sleeper.findSlot("FLEX").orElseThrow().count());
    }

    @Test
    void getFormats_fromTheTable_isTheSameJsonAsTheFormatData() {
        assertEquals(json.writeValueAsString(StandardRosterFormats.all()),
            json.writeValueAsString(rosterService.getFormats()));
    }

    // ---------- Settings ----------

    @Test
    void settings_startOnEspnStandard_andASwitchIsSavedToTheOneRow() {
        assertEquals(ESPN_STANDARD, rosterService.getSettings().formatId());

        rosterService.updateSettings(new RosterSettings(YAHOO_STANDARD));
        flushAndClear();

        assertEquals(1, settingsRepository.count());
        assertEquals(YAHOO_STANDARD, settingsRepository.findById(RosterSettings.ID).orElseThrow().formatId());
        assertEquals("{\"formatId\":\"yahoo-standard\"}", json.writeValueAsString(rosterService.getSettings()));
    }

    // ---------- Roster entries ----------

    @Test
    void addEditMoveRemove_areAllSavedToTheTable() {
        Player allen = savePlayer("Josh Allen", Position.QB);

        // Add
        Long id = rosterEntryService.createEntry(new RosterEntry(null, allen.getId(), null, "Starter")).getId();
        flushAndClear();
        RosterEntry added = rosterEntryRepository.findById(id).orElseThrow();
        assertEquals(RosterEntry.BENCH, added.getSlot());
        assertEquals("Josh Allen", added.getPlayer().getName());

        // Edit the note
        rosterEntryService.updateEntry(id, new RosterEntry(null, null, null, "Monitor snaps"));
        // Move
        rosterEntryService.moveEntry(id, "QB");
        flushAndClear();
        RosterEntry moved = rosterEntryRepository.findById(id).orElseThrow();
        assertEquals("QB", moved.getSlot());
        assertEquals("Monitor snaps", moved.getNote());
        assertEquals(allen.getId(), moved.getPlayerId());

        // Remove
        rosterEntryService.deleteEntry(id);
        flushAndClear();
        assertTrue(rosterEntryRepository.findById(id).isEmpty());
    }

    @Test
    void savedEntry_hasTheSameJsonShapeAsBefore() {
        Player allen = savePlayer("Josh Allen", Position.QB);
        Long id = rosterEntryService.createEntry(new RosterEntry(null, allen.getId(), null, "x")).getId();
        flushAndClear();

        String body = json.writeValueAsString(rosterEntryService.getAllEntries());

        assertEquals("[{\"id\":" + id + ",\"playerId\":" + allen.getId()
            + ",\"slot\":\"BENCH\",\"note\":\"x\"}]", body);
    }

    @Test
    void theRosterRules_stillApply() {
        Player allen = savePlayer("Josh Allen", Position.QB);
        Long id = rosterEntryService.createEntry(new RosterEntry(null, allen.getId(), null, "")).getId();
        flushAndClear();

        assertThrows(PlayerAlreadyOnRosterException.class,
            () -> rosterEntryService.createEntry(new RosterEntry(null, allen.getId(), null, "")));
        assertThrows(SlotNotAllowedException.class, () -> rosterEntryService.moveEntry(id, "RB"));
    }

    @Test
    void foreignKey_stopsARosteredPlayerBeingDeleted() {
        Player allen = savePlayer("Josh Allen", Position.QB);
        rosterEntryService.createEntry(new RosterEntry(null, allen.getId(), null, ""));
        flushAndClear();

        playerRepository.deleteById(allen.getId());

        assertThrows(DataIntegrityViolationException.class, () -> playerRepository.flush());
    }
}
