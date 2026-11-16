package com.sidelined.backend.controller;

import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Player;
import com.sidelined.backend.model.Position;
import com.sidelined.backend.model.Workload;
import com.sidelined.backend.model.WorkloadEntry;
import com.sidelined.backend.repository.PlayerRepository;
import com.sidelined.backend.repository.WorkloadRepository;
import com.sidelined.backend.service.WorkloadService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// The JSON the front end reads: one entry per game in week order, no entry for a bye.
class WorkloadControllerTest {

    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final WorkloadRepository workloadRepository = mock(WorkloadRepository.class);
    private final MockMvc mvc = MockMvcBuilders
        .standaloneSetup(new WorkloadController(new WorkloadService(workloadRepository, playerRepository)))
        .setControllerAdvice(new ApiExceptionHandler())
        .build();

    // The one stored player: Jacoby Brissett (id 1), with three games around a week 2 bye.
    WorkloadControllerTest() {
        Player brissett = new Player(1L, "Jacoby Brissett", NflTeam.ARI, Position.QB, new Workload(0, 0));
        when(playerRepository.existsById(1L)).thenReturn(true);
        when(workloadRepository.findByPlayerId(1L)).thenReturn(List.of(
            new WorkloadEntry(brissett, 2026, 1, 61),
            new WorkloadEntry(brissett, 2026, 3, 64),
            new WorkloadEntry(brissett, 2026, 4, 68)));
    }

    @Test
    void knownPlayer_returnsGamesInWeekOrder_withNoEntryForTheBye() throws Exception {
        mvc.perform(get("/players/1/workload"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].week").value(1))
            .andExpect(jsonPath("$[1].week").value(3))
            .andExpect(jsonPath("$[2].week").value(4))
            .andExpect(jsonPath("$[2].snaps").value(68));
    }

    @Test
    void json_keepsItsShape_playerIdWeekAndSnapsOnly() throws Exception {
        // Strict: no id, season or player, though the table has them.
        mvc.perform(get("/players/1/workload"))
            .andExpect(content().json("[{\"playerId\":1,\"week\":1,\"snaps\":61},"
                + "{\"playerId\":1,\"week\":3,\"snaps\":64},"
                + "{\"playerId\":1,\"week\":4,\"snaps\":68}]", true));
    }

    @Test
    void unknownPlayer_returns404() throws Exception {
        mvc.perform(get("/players/9999/workload"))
            .andExpect(status().isNotFound());
    }
}
