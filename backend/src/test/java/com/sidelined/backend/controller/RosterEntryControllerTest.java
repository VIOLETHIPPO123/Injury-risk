package com.sidelined.backend.controller;

import com.sidelined.backend.exception.NoActiveInjuryException;
import com.sidelined.backend.exception.NotFantasyEligibleException;
import com.sidelined.backend.exception.PlayerAlreadyOnRosterException;
import com.sidelined.backend.exception.PlayerNotFoundException;
import com.sidelined.backend.exception.RosterEntryNotFoundException;
import com.sidelined.backend.exception.RosterFullException;
import com.sidelined.backend.exception.SlotFullException;
import com.sidelined.backend.exception.SlotNotAllowedException;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.service.RosterEntryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// The roster's HTTP behavior: routes, JSON shapes and which error becomes which status.
// The rules themselves are tested one by one in the validation package, and all together
// against a real database in RosterPersistenceTest.
class RosterEntryControllerTest {

    // The "sub" of the signed-in user\'s token.
    private static final String USER = "user-a";

    private final RosterEntryService rosterEntryService = mock(RosterEntryService.class);
    private final MockMvc mvc = MockMvcBuilders
        .standaloneSetup(new RosterEntryController(rosterEntryService))
        .setControllerAdvice(new ApiExceptionHandler())
        // Lets @AuthenticationPrincipal read the token jwt() puts in the security context.
        .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
        // Every request is signed in as USER unless it says otherwise.
        .defaultRequest(get("/").with(signedInAs(USER)))
        .build();

    // Signs the request in as a verified Supabase token with this "sub". A standalone MockMvc has no
    // security filters, so this puts the token straight where @AuthenticationPrincipal reads it.
    private static RequestPostProcessor signedInAs(String userId) {
        return request -> {
            Jwt token = Jwt.withTokenValue("test-token").header("alg", "none").subject(userId).build();
            SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(token));
            return request;
        };
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    private ResultActions add(long playerId) throws Exception {
        return mvc.perform(post("/roster").contentType(MediaType.APPLICATION_JSON)
            .content("{\"playerId\":" + playerId + ",\"note\":\"\"}"));
    }

    private ResultActions move(long entryId, String slot) throws Exception {
        return mvc.perform(put("/roster/" + entryId + "/slot").contentType(MediaType.APPLICATION_JSON)
            .content("{\"slot\":\"" + slot + "\"}"));
    }

    // ---------- Reading ----------

    @Test
    void getRoster_returnsEntriesInTheirJsonShape() throws Exception {
        when(rosterEntryService.getAllEntries(USER))
            .thenReturn(List.of(new RosterEntry(1L, 28L, "TE", "Monitor snaps")));

        mvc.perform(get("/roster"))
            .andExpect(status().isOk())
            .andExpect(content().json(
                "[{\"id\":1,\"playerId\":28,\"slot\":\"TE\",\"note\":\"Monitor snaps\"}]",
                JsonCompareMode.STRICT));
    }

    // ---------- Adding ----------

    @Test
    void addKnownPlayer_returns201_onTheBench() throws Exception {
        when(rosterEntryService.createEntry(eq(USER), any())).thenReturn(new RosterEntry(1L, 1L, RosterEntry.BENCH, ""));
        ArgumentCaptor<RosterEntry> captor = ArgumentCaptor.forClass(RosterEntry.class);

        add(1L)
            .andExpect(status().isCreated())
            .andExpect(content().json(
                "{\"id\":1,\"playerId\":1,\"slot\":\"BENCH\",\"note\":\"\"}", JsonCompareMode.STRICT));

        verify(rosterEntryService).createEntry(eq(USER), captor.capture());
        assertEquals(1L, captor.getValue().getPlayerId());
        assertEquals("", captor.getValue().getNote());
    }

    @Test
    void addUnknownPlayer_returns404() throws Exception {
        when(rosterEntryService.createEntry(eq(USER), any())).thenThrow(new PlayerNotFoundException(9999L));

        add(9999L)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Player not found: 9999"));
    }

    @Test
    void addPlayerAlreadyOnRoster_returns409() throws Exception {
        when(rosterEntryService.createEntry(eq(USER), any())).thenThrow(new PlayerAlreadyOnRosterException(1L));

        add(1L).andExpect(status().isConflict());
    }

    @Test
    void addIneligiblePosition_returns400() throws Exception {
        when(rosterEntryService.createEntry(eq(USER), any())).thenThrow(new NotFantasyEligibleException());

        add(7L)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Not a fantasy-eligible position."));
    }

    @Test
    void addPastTheFormatsMaximum_returns409() throws Exception {
        when(rosterEntryService.createEntry(eq(USER), any())).thenThrow(new RosterFullException(16));

        add(34L).andExpect(status().isConflict());
    }

    // ---------- Moving ----------

    @Test
    void moveIntoAnAllowedSlot_returns200() throws Exception {
        when(rosterEntryService.moveEntry(USER, 1L, "QB")).thenReturn(new RosterEntry(1L, 1L, "QB", ""));

        move(1, "QB")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slot").value("QB"));
    }

    @Test
    void moveRejectedByARule_returns409() throws Exception {
        when(rosterEntryService.moveEntry(eq(USER), eq(1L), any()))
            .thenThrow(new SlotNotAllowedException("A QB can't play RB"));
        when(rosterEntryService.moveEntry(USER, 2L, "QB")).thenThrow(new SlotFullException("QB"));
        when(rosterEntryService.moveEntry(USER, 3L, "IR")).thenThrow(new NoActiveInjuryException(1L));

        move(1, "RB").andExpect(status().isConflict());
        move(2, "QB").andExpect(status().isConflict());
        move(3, "IR").andExpect(status().isConflict());
    }

    @Test
    void moveMissingEntry_returns404() throws Exception {
        when(rosterEntryService.moveEntry(USER, 999L, "BENCH")).thenThrow(new RosterEntryNotFoundException(999L));

        move(999, "BENCH").andExpect(status().isNotFound());
    }

    // ---------- Notes and removing ----------

    @Test
    void updateNote_returnsTheUpdatedEntry() throws Exception {
        when(rosterEntryService.updateEntry(eq(USER), eq(1L), any())).thenReturn(new RosterEntry(1L, 1L, "QB", "Monitor snaps"));
        ArgumentCaptor<RosterEntry> captor = ArgumentCaptor.forClass(RosterEntry.class);

        mvc.perform(put("/roster/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"Monitor snaps\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.note").value("Monitor snaps"))
            .andExpect(jsonPath("$.slot").value("QB"));

        verify(rosterEntryService).updateEntry(eq(USER), eq(1L), captor.capture());
        assertEquals("Monitor snaps", captor.getValue().getNote());
    }

    @Test
    void updateMissingEntry_returns404() throws Exception {
        when(rosterEntryService.updateEntry(eq(USER), eq(999L), any())).thenThrow(new RosterEntryNotFoundException(999L));

        mvc.perform(put("/roster/999").contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"x\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteEntry_returns204() throws Exception {
        mvc.perform(delete("/roster/1")).andExpect(status().isNoContent());

        verify(rosterEntryService).deleteEntry(USER, 1L);
    }

    @Test
    void deleteMissingEntry_returns404() throws Exception {
        doThrow(new RosterEntryNotFoundException(999L)).when(rosterEntryService).deleteEntry(USER, 999L);

        mvc.perform(delete("/roster/999")).andExpect(status().isNotFound());
    }

    // ---------- Whose roster ----------

    @Test
    void everyRoute_usesTheSignedInUsersSub_notAnythingInTheRequest() throws Exception {
        String other = "user-b";
        when(rosterEntryService.getAllEntries(other)).thenReturn(List.of());

        mvc.perform(get("/roster").with(signedInAs(other))).andExpect(status().isOk());
        mvc.perform(post("/roster").with(signedInAs(other)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"playerId\":1,\"note\":\"\"}"));
        mvc.perform(put("/roster/1/slot").with(signedInAs(other)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"slot\":\"QB\"}"));
        mvc.perform(put("/roster/1").with(signedInAs(other)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"note\":\"x\"}"));
        mvc.perform(delete("/roster/1").with(signedInAs(other)));

        verify(rosterEntryService).getAllEntries(other);
        verify(rosterEntryService).createEntry(eq(other), any());
        verify(rosterEntryService).moveEntry(other, 1L, "QB");
        verify(rosterEntryService).updateEntry(eq(other), eq(1L), any());
        verify(rosterEntryService).deleteEntry(other, 1L);
        verifyNoMoreInteractions(rosterEntryService);
    }

    @Test
    void anotherUsersEntry_looksExactlyLikeAMissingOne() throws Exception {
        // The service can't find entry 5 for this user, because it is on someone else's roster.
        doThrow(new RosterEntryNotFoundException(5L)).when(rosterEntryService).deleteEntry(USER, 5L);
        when(rosterEntryService.moveEntry(USER, 5L, "QB")).thenThrow(new RosterEntryNotFoundException(5L));

        mvc.perform(delete("/roster/5"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Roster entry not found: 5"));
        move(5, "QB")
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Roster entry not found: 5"));
    }
}
