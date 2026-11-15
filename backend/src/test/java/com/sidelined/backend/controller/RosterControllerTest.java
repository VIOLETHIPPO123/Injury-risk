package com.sidelined.backend.controller;

import com.sidelined.backend.exception.RosterTooBigForFormatException;
import com.sidelined.backend.exception.UnknownRosterFormatException;
import com.sidelined.backend.model.RosterSettings;
import com.sidelined.backend.model.StandardRosterFormats;
import com.sidelined.backend.service.RosterService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// The JSON the front end reads and the status codes it gets. The rules behind them are tested in
// RosterServiceTest; the format data in StandardRosterFormatsTest.
class RosterControllerTest {

    private final RosterService rosterService = mock(RosterService.class);
    private final MockMvc mvc = MockMvcBuilders
        .standaloneSetup(new RosterController(rosterService))
        .setControllerAdvice(new ApiExceptionHandler())
        .build();

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
        when(rosterService.getSettings()).thenReturn(RosterSettings.defaults());

        mvc.perform(get("/roster/settings"))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"formatId\":\"espn-standard\"}", JsonCompareMode.STRICT));
    }

    @Test
    void putKnownFormat_returns200_withTheSavedSettings() throws Exception {
        when(rosterService.updateSettings(any())).thenReturn(new RosterSettings("yahoo-standard"));
        ArgumentCaptor<RosterSettings> captor = ArgumentCaptor.forClass(RosterSettings.class);

        mvc.perform(put("/roster/settings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"formatId\":\"yahoo-standard\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.formatId").value("yahoo-standard"));

        verify(rosterService).updateSettings(captor.capture());
        assertEquals("yahoo-standard", captor.getValue().formatId());
    }

    @Test
    void putUnknownFormat_returns400() throws Exception {
        when(rosterService.updateSettings(any())).thenThrow(new UnknownRosterFormatException("nope"));

        mvc.perform(put("/roster/settings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"formatId\":\"nope\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Unknown roster format: nope"));
    }

    @Test
    void putWithNoFormat_passesANullFormatId() throws Exception {
        when(rosterService.updateSettings(any())).thenThrow(new UnknownRosterFormatException(null));
        ArgumentCaptor<RosterSettings> captor = ArgumentCaptor.forClass(RosterSettings.class);

        mvc.perform(put("/roster/settings").contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());

        verify(rosterService).updateSettings(captor.capture());
        assertNull(captor.getValue().formatId());
    }

    @Test
    void putFormatTooSmallForTheRoster_returns409() throws Exception {
        when(rosterService.updateSettings(any()))
            .thenThrow(new RosterTooBigForFormatException("Sleeper Default", 15, 16));

        mvc.perform(put("/roster/settings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"formatId\":\"sleeper-default\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("Sleeper Default holds 15 players; the roster has 16"));
    }
}
