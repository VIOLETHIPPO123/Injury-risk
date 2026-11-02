package com.sidelined.backend.controller;

import com.sidelined.backend.repository.InMemoryInjuryRepository;
import com.sidelined.backend.repository.InMemoryPlayerRepository;
import com.sidelined.backend.repository.InMemoryRosterEntryRepository;
import com.sidelined.backend.repository.InMemoryRosterFormatRepository;
import com.sidelined.backend.repository.InMemoryRosterSettingsRepository;
import com.sidelined.backend.service.CurrentRosterFormat;
import com.sidelined.backend.service.RosterEntryService;
import com.sidelined.backend.validation.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// The roster's HTTP behavior with every rule wired in, in the order Spring runs them.
// The league is on ESPN Standard (the default): 16 players plus 1 IR.
class RosterEntryControllerTest {

    private final InMemoryPlayerRepository playerRepository = new InMemoryPlayerRepository();
    private final InMemoryRosterEntryRepository rosterEntryRepository = new InMemoryRosterEntryRepository();
    private final CurrentRosterFormat currentFormat = new CurrentRosterFormat(
        new InMemoryRosterFormatRepository(), new InMemoryRosterSettingsRepository());

    private final MockMvc mvc = MockMvcBuilders
        .standaloneSetup(new RosterEntryController(new RosterEntryService(
            rosterEntryRepository,
            List.of(
                new PlayerExistsValidator(playerRepository),
                new FantasyEligibleValidator(playerRepository),
                new NotAlreadyOnRosterValidator(rosterEntryRepository),
                new RosterNotFullValidator(rosterEntryRepository, currentFormat)),
            List.of(
                new SlotAllowsPositionValidator(playerRepository, currentFormat),
                new SlotHasRoomValidator(rosterEntryRepository, currentFormat),
                new InjuredReserveValidator(new InMemoryInjuryRepository()),
                new LeavingIrNeedsRoomValidator(rosterEntryRepository, currentFormat)))))
        .setControllerAdvice(new ApiExceptionHandler())
        .build();

    // Entry ids start at 1 and go up in the order players are added.
    private ResultActions add(long playerId) throws Exception {
        return mvc.perform(post("/roster").contentType(MediaType.APPLICATION_JSON)
            .content("{\"playerId\":" + playerId + ",\"note\":\"\"}"));
    }

    private ResultActions move(long entryId, String slot) throws Exception {
        return mvc.perform(put("/roster/" + entryId + "/slot").contentType(MediaType.APPLICATION_JSON)
            .content("{\"slot\":\"" + slot + "\"}"));
    }

    // ---------- Adding ----------

    @Test
    void addKnownPlayer_returns201_onTheBench() throws Exception {
        add(1L)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.playerId").value(1))
            .andExpect(jsonPath("$.slot").value("BENCH"));

        mvc.perform(get("/roster"))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].slot").value("BENCH"));
    }

    @Test
    void addUnknownPlayer_returns404() throws Exception {
        add(9999L).andExpect(status().isNotFound());
    }

    @Test
    void addPlayerAlreadyOnRoster_returns409() throws Exception {
        add(1L);

        add(1L).andExpect(status().isConflict());
    }

    @Test
    void addIneligiblePosition_returns400() throws Exception {
        // LT, DE, CB, KR, PR, H
        for (long id : new long[] {7L, 391L, 492L, 813L, 770L, 769L}) {
            add(id)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Not a fantasy-eligible position."));
        }
    }

    @Test
    void addPastTheFormatsMaximum_returns409() throws Exception {
        // 16 QBs, RBs, WRs and TEs from ARI, ATL and BAL.
        for (long id : new long[] {1, 2, 3, 4, 5, 6, 12, 13, 14, 15, 16, 17, 23, 24, 25, 26}) {
            add(id).andExpect(status().isCreated());
        }

        add(34L).andExpect(status().isConflict());
    }

    // ---------- Moving ----------

    @Test
    void moveIntoAnAllowedSlot_returns200() throws Exception {
        add(1L); // QB

        move(1, "QB")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slot").value("QB"));
    }

    @Test
    void moveIntoASlotThePositionIsntAllowedIn_returns409() throws Exception {
        add(1L); // QB

        move(1, "RB").andExpect(status().isConflict());
    }

    @Test
    void moveIntoAFilledSlot_returns409() throws Exception {
        add(1L);  // QB
        add(12L); // QB
        move(1, "QB");

        move(2, "QB").andExpect(status().isConflict());
    }

    @Test
    void moveToIr_withoutAnActiveInjury_returns409() throws Exception {
        add(1L); // Jacoby Brissett: no injuries

        move(1, "IR").andExpect(status().isConflict());
    }

    @Test
    void moveToIr_withAnActiveInjury_returns200() throws Exception {
        add(28L); // Mark Andrews: an Active ankle sprain

        move(1, "IR")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slot").value("IR"));
    }

    @Test
    void moveMissingEntry_returns404() throws Exception {
        move(999, "BENCH").andExpect(status().isNotFound());
    }

    // ---------- Notes and removing (kept from the watchlist) ----------

    @Test
    void updateNote_keepsTheSlot() throws Exception {
        add(1L);
        move(1, "QB");

        mvc.perform(put("/roster/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"Monitor snaps\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.note").value("Monitor snaps"))
            .andExpect(jsonPath("$.slot").value("QB"));
    }

    @Test
    void updateMissingEntry_returns404() throws Exception {
        mvc.perform(put("/roster/999").contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"x\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteEntry_returns204() throws Exception {
        add(1L);

        mvc.perform(delete("/roster/1")).andExpect(status().isNoContent());
    }

    @Test
    void deleteMissingEntry_returns404() throws Exception {
        mvc.perform(delete("/roster/999")).andExpect(status().isNotFound());
    }
}
