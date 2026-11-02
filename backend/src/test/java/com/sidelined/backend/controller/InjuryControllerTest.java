package com.sidelined.backend.controller;

import com.sidelined.backend.repository.InMemoryInjuryRepository;
import com.sidelined.backend.repository.InMemoryPlayerRepository;
import com.sidelined.backend.service.InjuryService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// The JSON the front end reads: field names and newest-first order.
class InjuryControllerTest {

    private final MockMvc mvc = MockMvcBuilders
        .standaloneSetup(new InjuryController(new InjuryService(
            new InMemoryInjuryRepository(), new InMemoryPlayerRepository())))
        .setControllerAdvice(new ApiExceptionHandler())
        .build();

    @Test
    void knownPlayer_returnsInjuriesNewestFirst_withEveryField() throws Exception {
        // Christian McCaffrey (id 321): newest is 2026 week 6.
        mvc.perform(get("/players/321/injuries"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4))
            .andExpect(jsonPath("$[0].season").value(2026))
            .andExpect(jsonPath("$[0].week").value(6))
            .andExpect(jsonPath("$[0].bodyPart").value("Calf"))
            .andExpect(jsonPath("$[0].type").value("Strain"))
            .andExpect(jsonPath("$[0].gamesMissed").value(1))
            .andExpect(jsonPath("$[0].status").value("Questionable"))
            .andExpect(jsonPath("$[3].season").value(2023));
    }

    @Test
    void playerWithNoInjuries_returnsEmptyList() throws Exception {
        mvc.perform(get("/players/1/injuries"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void unknownPlayer_returns404() throws Exception {
        mvc.perform(get("/players/9999/injuries"))
            .andExpect(status().isNotFound());
    }
}
