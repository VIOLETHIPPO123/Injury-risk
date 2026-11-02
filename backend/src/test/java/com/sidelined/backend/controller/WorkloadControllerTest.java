package com.sidelined.backend.controller;

import com.sidelined.backend.repository.InMemoryPlayerRepository;
import com.sidelined.backend.repository.InMemoryWorkloadRepository;
import com.sidelined.backend.service.WorkloadService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// The JSON the front end reads: one entry per game in week order, no entry for a bye.
class WorkloadControllerTest {

    private final InMemoryPlayerRepository playerRepository = new InMemoryPlayerRepository();
    private final MockMvc mvc = MockMvcBuilders
        .standaloneSetup(new WorkloadController(new WorkloadService(
            new InMemoryWorkloadRepository(playerRepository), playerRepository)))
        .setControllerAdvice(new ApiExceptionHandler())
        .build();

    @Test
    void knownPlayer_returnsGamesInWeekOrder_withNoEntryForTheBye() throws Exception {
        // Jacoby Brissett (id 1): ARI, bye in week 5, 68 snaps last game.
        mvc.perform(get("/players/1/workload"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(7))
            .andExpect(jsonPath("$[0].playerId").value(1))
            .andExpect(jsonPath("$[0].week").value(1))
            .andExpect(jsonPath("$[3].week").value(4))
            .andExpect(jsonPath("$[4].week").value(6))
            .andExpect(jsonPath("$[6].week").value(8))
            .andExpect(jsonPath("$[6].snaps").value(68));
    }

    @Test
    void unknownPlayer_returns404() throws Exception {
        mvc.perform(get("/players/9999/workload"))
            .andExpect(status().isNotFound());
    }
}
