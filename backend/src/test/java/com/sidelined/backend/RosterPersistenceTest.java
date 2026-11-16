package com.sidelined.backend;

import com.sidelined.backend.config.RosterDataLoader;
import com.sidelined.backend.exception.PlayerAlreadyOnRosterException;
import com.sidelined.backend.exception.RosterEntryNotFoundException;
import com.sidelined.backend.exception.RosterFullException;
import com.sidelined.backend.exception.SlotFullException;
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
import com.sidelined.backend.repository.ProfileRepository;
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
import java.util.ArrayList;
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

    // Two signed-in users (their tokens' "sub").
    private static final String ALICE = "user-alice";
    private static final String BOB = "user-bob";

    @Autowired private RosterEntryService rosterEntryService;
    @Autowired private RosterService rosterService;
    @Autowired private RosterDataLoader rosterDataLoader;
    @Autowired private JpaPlayerRepository playerRepository;
    @Autowired private JpaRosterEntryRepository rosterEntryRepository;
    @Autowired private JpaRosterFormatRepository formatRepository;
    @Autowired private JpaRosterSettingsRepository settingsRepository;
    @Autowired private ProfileRepository profileRepository;
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
    void settings_startOnEspnStandard_andASwitchSavesTheUsersOwnRow() {
        assertEquals(ESPN_STANDARD, rosterService.getSettings(ALICE).formatId());
        assertEquals(0, settingsRepository.count());

        rosterService.updateSettings(ALICE, new RosterSettings(YAHOO_STANDARD));
        flushAndClear();
        // A second switch updates the same row.
        rosterService.updateSettings(ALICE, new RosterSettings(SLEEPER_DEFAULT));
        flushAndClear();

        assertEquals(1, settingsRepository.count());
        RosterSettings saved = settingsRepository.findByProfileId(ALICE).orElseThrow();
        assertEquals(SLEEPER_DEFAULT, saved.formatId());
        assertEquals(ALICE, saved.profile().getId());
        assertEquals("{\"formatId\":\"sleeper-default\"}", json.writeValueAsString(rosterService.getSettings(ALICE)));
    }

    // ---------- Roster entries ----------

    @Test
    void addEditMoveRemove_areAllSavedToTheTable() {
        Player allen = savePlayer("Josh Allen", Position.QB);

        // Add
        Long id = rosterEntryService.createEntry(ALICE, new RosterEntry(null, allen.getId(), null, "Starter")).getId();
        flushAndClear();
        RosterEntry added = rosterEntryRepository.findById(id).orElseThrow();
        assertEquals(RosterEntry.BENCH, added.getSlot());
        assertEquals("Josh Allen", added.getPlayer().getName());

        // Edit the note
        rosterEntryService.updateEntry(ALICE, id, new RosterEntry(null, null, null, "Monitor snaps"));
        // Move
        rosterEntryService.moveEntry(ALICE, id, "QB");
        flushAndClear();
        RosterEntry moved = rosterEntryRepository.findById(id).orElseThrow();
        assertEquals("QB", moved.getSlot());
        assertEquals("Monitor snaps", moved.getNote());
        assertEquals(allen.getId(), moved.getPlayerId());

        // Remove
        rosterEntryService.deleteEntry(ALICE, id);
        flushAndClear();
        assertTrue(rosterEntryRepository.findById(id).isEmpty());
    }

    @Test
    void savedEntry_hasTheSameJsonShapeAsBefore() {
        Player allen = savePlayer("Josh Allen", Position.QB);
        Long id = rosterEntryService.createEntry(ALICE, new RosterEntry(null, allen.getId(), null, "x")).getId();
        flushAndClear();

        String body = json.writeValueAsString(rosterEntryService.getAllEntries(ALICE));

        assertEquals("[{\"id\":" + id + ",\"playerId\":" + allen.getId()
            + ",\"slot\":\"BENCH\",\"note\":\"x\"}]", body);
    }

    @Test
    void theRosterRules_stillApply() {
        Player allen = savePlayer("Josh Allen", Position.QB);
        Long id = rosterEntryService.createEntry(ALICE, new RosterEntry(null, allen.getId(), null, "")).getId();
        flushAndClear();

        assertThrows(PlayerAlreadyOnRosterException.class,
            () -> rosterEntryService.createEntry(ALICE, new RosterEntry(null, allen.getId(), null, "")));
        assertThrows(SlotNotAllowedException.class, () -> rosterEntryService.moveEntry(ALICE, id, "RB"));
    }

    @Test
    void foreignKey_stopsARosteredPlayerBeingDeleted() {
        Player allen = savePlayer("Josh Allen", Position.QB);
        rosterEntryService.createEntry(ALICE, new RosterEntry(null, allen.getId(), null, ""));
        flushAndClear();

        playerRepository.deleteById(allen.getId());

        assertThrows(DataIntegrityViolationException.class, () -> playerRepository.flush());
    }

    @Test
    void addingAPlayer_linksTheEntryToTheUsersProfileRow() {
        Player allen = savePlayer("Josh Allen", Position.QB);

        Long id = rosterEntryService.createEntry(ALICE, new RosterEntry(null, allen.getId(), null, "")).getId();
        flushAndClear();

        assertTrue(profileRepository.existsById(ALICE));
        assertEquals(ALICE, rosterEntryRepository.findById(id).orElseThrow().getProfile().getId());
    }

    // ---------- Two users ----------

    @Test
    void twoUsers_haveSeparateRosters_andCanBothRosterTheSamePlayer() {
        Player allen = savePlayer("Josh Allen", Position.QB);
        Player cook = savePlayer("James Cook", Position.RB);

        rosterEntryService.createEntry(ALICE, new RosterEntry(null, allen.getId(), null, "Alice's pick"));
        rosterEntryService.createEntry(BOB, new RosterEntry(null, allen.getId(), null, "Bob's pick"));
        rosterEntryService.createEntry(BOB, new RosterEntry(null, cook.getId(), null, ""));
        flushAndClear();

        assertEquals(List.of("Alice's pick"),
            rosterEntryService.getAllEntries(ALICE).stream().map(RosterEntry::getNote).toList());
        assertEquals(List.of(allen.getId(), cook.getId()),
            rosterEntryService.getAllEntries(BOB).stream().map(RosterEntry::getPlayerId).toList());
        assertTrue(rosterEntryService.getAllEntries("user-carol").isEmpty());
    }

    @Test
    void twoUsers_canChooseDifferentFormats_andASwitchOnlyBenchesTheSwitchersPlayers() {
        Player allen = savePlayer("Josh Allen", Position.QB);
        Long alicesQb = rosterEntryService.createEntry(ALICE, new RosterEntry(null, allen.getId(), null, "")).getId();
        Long bobsQb = rosterEntryService.createEntry(BOB, new RosterEntry(null, allen.getId(), null, "")).getId();

        rosterService.updateSettings(ALICE, new RosterSettings(YAHOO_STANDARD));
        rosterService.updateSettings(BOB, new RosterSettings(SLEEPER_DEFAULT));
        flushAndClear();

        assertEquals(YAHOO_STANDARD, rosterService.getSettings(ALICE).formatId());
        assertEquals(SLEEPER_DEFAULT, rosterService.getSettings(BOB).formatId());
        // Someone who has never chosen is still on the default.
        assertEquals(ESPN_STANDARD, rosterService.getSettings("user-carol").formatId());
        assertEquals(2, settingsRepository.count());

        // Both start their QB, then only Bob switches format: only Bob's QB goes to the bench.
        rosterEntryService.moveEntry(ALICE, alicesQb, "QB");
        rosterEntryService.moveEntry(BOB, bobsQb, "QB");
        flushAndClear();
        rosterService.updateSettings(BOB, new RosterSettings(ESPN_STANDARD));
        flushAndClear();

        assertEquals("QB", rosterEntryRepository.findById(alicesQb).orElseThrow().getSlot());
        assertEquals(RosterEntry.BENCH, rosterEntryRepository.findById(bobsQb).orElseThrow().getSlot());
    }

    @Test
    void changingAnotherUsersEntry_isNotFound_exactlyLikeAnIdThatDoesntExist() {
        Player allen = savePlayer("Josh Allen", Position.QB);
        Long alicesEntry = rosterEntryService.createEntry(ALICE, new RosterEntry(null, allen.getId(), null, "Mine")).getId();
        flushAndClear();

        RosterEntryNotFoundException edit = assertThrows(RosterEntryNotFoundException.class,
            () -> rosterEntryService.updateEntry(BOB, alicesEntry, new RosterEntry(null, null, null, "Bob was here")));
        assertThrows(RosterEntryNotFoundException.class, () -> rosterEntryService.moveEntry(BOB, alicesEntry, "QB"));
        assertThrows(RosterEntryNotFoundException.class, () -> rosterEntryService.deleteEntry(BOB, alicesEntry));
        RosterEntryNotFoundException missing = assertThrows(RosterEntryNotFoundException.class,
            () -> rosterEntryService.deleteEntry(BOB, 999_999L));
        flushAndClear();

        // The same error and wording as a made-up id, so Bob can't tell the entry exists.
        assertEquals("Roster entry not found: " + alicesEntry, edit.getMessage());
        assertEquals("Roster entry not found: 999999", missing.getMessage());
        // And Alice's entry is untouched.
        RosterEntry stillAlices = rosterEntryRepository.findById(alicesEntry).orElseThrow();
        assertEquals("Mine", stillAlices.getNote());
        assertEquals(RosterEntry.BENCH, stillAlices.getSlot());
    }

    @Test
    void oneUsersFullRoster_doesntStopAnotherUserAdding() {
        // ESPN Standard holds 16. Fill Alice's roster.
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < 17; i++) {
            players.add(savePlayer("Player " + i, Position.WR));
        }
        for (int i = 0; i < 16; i++) {
            rosterEntryService.createEntry(ALICE, new RosterEntry(null, players.get(i).getId(), null, ""));
        }
        flushAndClear();
        Long seventeenth = players.get(16).getId();

        assertThrows(RosterFullException.class,
            () -> rosterEntryService.createEntry(ALICE, new RosterEntry(null, seventeenth, null, "")));
        assertDoesNotThrow(() -> rosterEntryService.createEntry(BOB, new RosterEntry(null, seventeenth, null, "")));
        assertEquals(1, rosterEntryService.getAllEntries(BOB).size());
    }

    @Test
    void oneUsersFullSlot_doesntStopAnotherUserFillingTheirs() {
        Player allen = savePlayer("Josh Allen", Position.QB);
        Player burrow = savePlayer("Joe Burrow", Position.QB);
        Long alicesAllen = rosterEntryService.createEntry(ALICE, new RosterEntry(null, allen.getId(), null, "")).getId();
        Long alicesBurrow = rosterEntryService.createEntry(ALICE, new RosterEntry(null, burrow.getId(), null, "")).getId();
        Long bobsBurrow = rosterEntryService.createEntry(BOB, new RosterEntry(null, burrow.getId(), null, "")).getId();
        rosterEntryService.moveEntry(ALICE, alicesAllen, "QB");
        flushAndClear();

        // Alice's one QB slot is taken...
        assertThrows(SlotFullException.class, () -> rosterEntryService.moveEntry(ALICE, alicesBurrow, "QB"));
        // ...but Bob's isn't.
        assertDoesNotThrow(() -> rosterEntryService.moveEntry(BOB, bobsBurrow, "QB"));
    }
}
