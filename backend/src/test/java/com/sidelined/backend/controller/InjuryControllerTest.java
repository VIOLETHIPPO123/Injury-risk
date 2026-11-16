package com.sidelined.backend.controller;

import com.sidelined.backend.model.Injury;
import com.sidelined.backend.repository.InjuryRepository;
import com.sidelined.backend.repository.PlayerRepository;
import com.sidelined.backend.service.InjuryService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;

import static com.sidelined.backend.model.InjuryStatus.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// The JSON the front end reads: field names and newest-first order. How injuries are worked
// out from the weekly reports is tested in InjuryHistoryTest.
class InjuryControllerTest {

    private final InjuryRepository injuryRepository = mock(InjuryRepository.class);
    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final MockMvc mvc = MockMvcBuilders
        .standaloneSetup(new InjuryController(new InjuryService(injuryRepository, playerRepository)))
        .setControllerAdvice(new ApiExceptionHandler())
        .build();

    InjuryControllerTest() {
        when(playerRepository.existsById(321L)).thenReturn(true);
        when(playerRepository.existsById(1L)).thenReturn(true);
        when(injuryRepository.findByPlayerId(321L)).thenReturn(List.of(
            new Injury(11L, 321L, 2024, 1, "Achilles", null, 8, RECOVERED),
            new Injury(40L, 321L, 2026, 9, "Calf", null, 1, ACTIVE),
            new Injury(25L, 321L, 2025, 12, "Hamstring", null, 2, RECOVERED),
            new Injury(21L, 321L, 2025, 3, "Hamstring", null, 0, RECOVERED)));
        when(injuryRepository.findByPlayerId(1L)).thenReturn(List.of());
    }

    @Test
    void knownPlayer_returnsInjuriesNewestFirst_withEveryField() throws Exception {
        mvc.perform(get("/players/321/injuries"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4))
            .andExpect(jsonPath("$[0].id").value(40))
            .andExpect(jsonPath("$[0].playerId").value(321))
            .andExpect(jsonPath("$[0].season").value(2026))
            .andExpect(jsonPath("$[0].week").value(9))
            .andExpect(jsonPath("$[0].bodyPart").value("Calf"))
            // Still in the JSON, but the injury reports have no type.
            .andExpect(jsonPath("$[0].type").isEmpty())
            .andExpect(jsonPath("$[0].gamesMissed").value(1))
            .andExpect(jsonPath("$[0].status").value("Active"))
            .andExpect(jsonPath("$[1].week").value(12))
            .andExpect(jsonPath("$[2].week").value(3))
            .andExpect(jsonPath("$[3].season").value(2024))
            .andExpect(jsonPath("$[3].status").value("Recovered"));
    }

    @Test
    void injury_hasTheSameFieldsAsBefore() throws Exception {
        mvc.perform(get("/players/321/injuries"))
            .andExpect(content().string(org.hamcrest.Matchers.startsWith(
                "[{\"id\":40,\"playerId\":321,\"season\":2026,\"week\":9,\"bodyPart\":\"Calf\",\"type\":null,"
                    + "\"gamesMissed\":1,\"status\":\"Active\"}")));
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
