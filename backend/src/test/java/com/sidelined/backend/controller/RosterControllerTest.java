package com.sidelined.backend.controller;

import com.sidelined.backend.repository.InMemoryRosterFormatRepository;
import com.sidelined.backend.repository.InMemoryRosterSettingsRepository;
import com.sidelined.backend.service.RosterService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// The JSON the front end reads. The format counts themselves are tested in InMemoryRosterFormatRepositoryTest.
class RosterControllerTest {

    private final MockMvc mvc = MockMvcBuilders
        .standaloneSetup(new RosterController(new RosterService(
            new InMemoryRosterFormatRepository(), new InMemoryRosterSettingsRepository())))
        .setControllerAdvice(new ApiExceptionHandler())
        .build();

    @Test
    void getFormats_returnsTheThreeFormats() throws Exception {
        mvc.perform(get("/roster/formats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].id").value("espn-standard"))
            .andExpect(jsonPath("$[0].name").value("ESPN Standard"))
            .andExpect(jsonPath("$[0].slots[0].name").value("QB"))
            .andExpect(jsonPath("$[0].slots[0].count").value(1))
            .andExpect(jsonPath("$[0].slots[0].positions[0]").value("QB"))
            .andExpect(jsonPath("$[0].benchCount").value(7))
            .andExpect(jsonPath("$[0].irCount").value(1))
            .andExpect(jsonPath("$[0].maxRosterSize").value(16))
            .andExpect(jsonPath("$[1].id").value("yahoo-standard"))
            .andExpect(jsonPath("$[2].id").value("sleeper-default"));
    }

    @Test
    void getSettings_defaultsToEspnStandard() throws Exception {
        mvc.perform(get("/roster/settings"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.formatId").value("espn-standard"));
    }

    @Test
    void putKnownFormat_returns200_andIsKept() throws Exception {
        mvc.perform(put("/roster/settings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"formatId\":\"yahoo-standard\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.formatId").value("yahoo-standard"));

        mvc.perform(get("/roster/settings"))
            .andExpect(jsonPath("$.formatId").value("yahoo-standard"));
    }

    @Test
    void putUnknownFormat_returns400() throws Exception {
        mvc.perform(put("/roster/settings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"formatId\":\"nope\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Unknown roster format: nope"));
    }

    @Test
    void putWithNoFormat_returns400() throws Exception {
        mvc.perform(put("/roster/settings").contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }
}
