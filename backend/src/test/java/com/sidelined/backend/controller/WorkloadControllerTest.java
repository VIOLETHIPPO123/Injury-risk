package com.sidelined.backend.controller;

import com.sidelined.backend.repository.InMemoryPlayerRepository;
import com.sidelined.backend.repository.InMemoryWorkloadRepository;
import com.sidelined.backend.service.WorkloadService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// The JSON the front end reads: one entry per week, a bye as "snaps": null.
class WorkloadControllerTest {

    private final InMemoryPlayerRepository playerRepository = new InMemoryPlayerRepository();
    private final MockMvc mvc = MockMvcBuilders
        .standaloneSetup(new WorkloadController(new WorkloadService(
            new InMemoryWorkloadRepository(playerRepository), playerRepository)))
        .setControllerAdvice(new ApiExceptionHandler())
        .build();

    @Test
    void knownPlayer_returnsEveryWeekOldestFirst_withTheByeAsNull() throws Exception {
        // Jacoby Brissett (id 1): ARI, bye in week 5, 68 snaps last game.
        mvc.perform(get("/players/1/workload"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(8))
            .andExpect(jsonPath("$[0].week").value(1))
            .andExpect(jsonPath("$[4].week").value(5))
            .andExpect(jsonPath("$[4].snaps").value(nullValue()))
            .andExpect(jsonPath("$[7].week").value(8))
            .andExpect(jsonPath("$[7].snaps").value(68));
    }

    @Test
    void unknownPlayer_returns404() throws Exception {
        mvc.perform(get("/players/9999/workload"))
            .andExpect(status().isNotFound());
    }
}
