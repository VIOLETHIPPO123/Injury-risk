package com.sidelined.backend.controller;

import com.sidelined.backend.repository.InMemoryWatchlistRepository;
import com.sidelined.backend.service.WatchlistService;
import com.sidelined.backend.validation.PlayerExistsValidator;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Proves the HTTP behavior is unchanged: domain exceptions still come out as 404s.
class WatchlistControllerErrorTest {

    private final MockMvc mvc = MockMvcBuilders
        .standaloneSetup(new WatchlistController(new WatchlistService(
            new InMemoryWatchlistRepository(),
            List.of(new PlayerExistsValidator(id -> id == 1L)))))
        .setControllerAdvice(new ApiExceptionHandler())
        .build();

    @Test
    void addUnknownPlayer_returns404() throws Exception {
        mvc.perform(post("/watchlist").contentType(MediaType.APPLICATION_JSON)
                .content("{\"playerId\":9999,\"note\":\"\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void addKnownPlayer_returns201() throws Exception {
        mvc.perform(post("/watchlist").contentType(MediaType.APPLICATION_JSON)
                .content("{\"playerId\":1,\"note\":\"\"}"))
            .andExpect(status().isCreated());
    }

    @Test
    void updateMissingEntry_returns404() throws Exception {
        mvc.perform(put("/watchlist/999").contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"x\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteMissingEntry_returns404() throws Exception {
        mvc.perform(delete("/watchlist/999")).andExpect(status().isNotFound());
    }
}
