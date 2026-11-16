package com.sidelined.backend.controller;

import com.sidelined.backend.repository.InMemoryInjuryRepository;
import com.sidelined.backend.repository.PlayerRepository;
import com.sidelined.backend.service.InjuryService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// The JSON the front end reads: field names and newest-first order.
class InjuryControllerTest {

    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final MockMvc mvc = MockMvcBuilders
        .standaloneSetup(new InjuryController(new InjuryService(
            new InMemoryInjuryRepository(), playerRepository)))
        .setControllerAdvice(new ApiExceptionHandler())
        .build();

    // The players the sample injuries belong to, and one (id 1) with none.
    InjuryControllerTest() {
        for (long id : new long[] {1L, 28L, 70L, 321L}) {
            when(playerRepository.existsById(id)).thenReturn(true);
        }
    }

    @Test
    void knownPlayer_returnsInjuriesNewestFirst_withEveryField() throws Exception {
        // Christian McCaffrey (id 321): newest is 2026 week 6.
        mvc.perform(get("/players/321/injuries"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4))
            .andExpect(jsonPath("$[0].id").value(4))
            .andExpect(jsonPath("$[0].playerId").value(321))
            .andExpect(jsonPath("$[0].season").value(2026))
            .andExpect(jsonPath("$[0].week").value(6))
            .andExpect(jsonPath("$[0].bodyPart").value("Calf"))
            .andExpect(jsonPath("$[0].type").value("Strain"))
            .andExpect(jsonPath("$[0].gamesMissed").value(1))
            .andExpect(jsonPath("$[0].status").value("Recovering"))
            .andExpect(jsonPath("$[1].season").value(2025))
            .andExpect(jsonPath("$[2].season").value(2024))
            .andExpect(jsonPath("$[3].season").value(2023))
            .andExpect(jsonPath("$[3].status").value("Recovered"));
    }

    @Test
    void newestFirst_withinASeason_goesByWeek() throws Exception {
        // Tee Higgins (id 70): 2026 week 7, then 2025 week 6.
        mvc.perform(get("/players/70/injuries"))
            .andExpect(jsonPath("$[0].week").value(7))
            .andExpect(jsonPath("$[1].week").value(6));
    }

    @Test
    void activeInjury_showsItsStatusAsActive() throws Exception {
        // Mark Andrews (id 28)
        mvc.perform(get("/players/28/injuries"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("Active"));
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
