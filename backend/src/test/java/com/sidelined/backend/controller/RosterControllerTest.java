package com.sidelined.backend.controller;

import com.sidelined.backend.exception.RosterTooBigForFormatException;
import com.sidelined.backend.exception.UnknownRosterFormatException;
import com.sidelined.backend.model.RosterSettings;
import com.sidelined.backend.model.StandardRosterFormats;
import com.sidelined.backend.service.RosterService;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// The JSON the front end reads and the status codes it gets. The rules behind them are tested in
// RosterServiceTest; the format data in StandardRosterFormatsTest.
class RosterControllerTest {

    // The "sub" of the signed-in user\'s token.
    private static final String USER = "user-a";

    private final RosterService rosterService = mock(RosterService.class);
    private final MockMvc mvc = MockMvcBuilders
        .standaloneSetup(new RosterController(rosterService))
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

    @Test
    void getFormats_returnsTheFormatsInTheirJsonShape() throws Exception {
        when(rosterService.getFormats()).thenReturn(StandardRosterFormats.all());

        mvc.perform(get("/roster/formats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].id").value("espn-standard"))
            .andExpect(jsonPath("$[0].name").value("ESPN Standard"))
            .andExpect(jsonPath("$[0].slots[0].name").value("QB"))
            .andExpect(jsonPath("$[0].slots[0].count").value(1))
            .andExpect(jsonPath("$[0].slots[0].positions[0]").value("QB"))
            .andExpect(jsonPath("$[0].slots[6].positions").value(contains("RB", "WR", "TE")))
            .andExpect(jsonPath("$[0].benchCount").value(7))
            .andExpect(jsonPath("$[0].irCount").value(1))
            .andExpect(jsonPath("$[0].maxRosterSize").value(16))
            // displayOrder is only for sorting; it isn't part of the JSON.
            .andExpect(jsonPath("$[0].displayOrder").doesNotExist())
            .andExpect(jsonPath("$[1].id").value("yahoo-standard"))
            .andExpect(jsonPath("$[2].id").value("sleeper-default"));
    }

    @Test
    void getSettings_returnsJustTheFormatId() throws Exception {
        when(rosterService.getSettings(USER)).thenReturn(RosterSettings.defaults());

        mvc.perform(get("/roster/settings"))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"formatId\":\"espn-standard\"}", JsonCompareMode.STRICT));
    }

    @Test
    void putKnownFormat_returns200_withTheSavedSettings() throws Exception {
        when(rosterService.updateSettings(eq(USER), any())).thenReturn(new RosterSettings("yahoo-standard"));
        ArgumentCaptor<RosterSettings> captor = ArgumentCaptor.forClass(RosterSettings.class);

        mvc.perform(put("/roster/settings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"formatId\":\"yahoo-standard\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.formatId").value("yahoo-standard"));

        verify(rosterService).updateSettings(eq(USER), captor.capture());
        assertEquals("yahoo-standard", captor.getValue().formatId());
    }

    @Test
    void putUnknownFormat_returns400() throws Exception {
        when(rosterService.updateSettings(eq(USER), any())).thenThrow(new UnknownRosterFormatException("nope"));

        mvc.perform(put("/roster/settings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"formatId\":\"nope\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Unknown roster format: nope"));
    }

    @Test
    void putWithNoFormat_passesANullFormatId() throws Exception {
        when(rosterService.updateSettings(eq(USER), any())).thenThrow(new UnknownRosterFormatException(null));
        ArgumentCaptor<RosterSettings> captor = ArgumentCaptor.forClass(RosterSettings.class);

        mvc.perform(put("/roster/settings").contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());

        verify(rosterService).updateSettings(eq(USER), captor.capture());
        assertNull(captor.getValue().formatId());
    }

    @Test
    void putFormatTooSmallForTheRoster_returns409() throws Exception {
        when(rosterService.updateSettings(eq(USER), any()))
            .thenThrow(new RosterTooBigForFormatException("Sleeper Default", 15, 16));

        mvc.perform(put("/roster/settings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"formatId\":\"sleeper-default\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("Sleeper Default holds 15 players; the roster has 16"));
    }

    @Test
    void settings_areTheSignedInUsers() throws Exception {
        when(rosterService.getSettings("user-b")).thenReturn(new RosterSettings("sleeper-default"));

        mvc.perform(get("/roster/settings").with(signedInAs("user-b")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.formatId").value("sleeper-default"));

        mvc.perform(put("/roster/settings").with(signedInAs("user-b")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"formatId\":\"yahoo-standard\"}"));
        verify(rosterService).updateSettings(eq("user-b"), any());
        verify(rosterService, never()).getSettings(USER);
    }
}
