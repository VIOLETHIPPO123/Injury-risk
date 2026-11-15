package com.sidelined.backend.controller;

import com.jayway.jsonpath.JsonPath;
import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Player;
import com.sidelined.backend.model.Position;
import com.sidelined.backend.model.Workload;
import com.sidelined.backend.repository.JpaPlayerRepository;
import com.sidelined.backend.service.SupabaseAuthClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Two signed-in users over real HTTP: the whole app, the real security filters and the H2 database.
// jwt() stands in for a verified Supabase token, so the "sub" it carries is what decides whose
// roster each request touches.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RosterOwnershipHttpTest {

    @Autowired private MockMvc mvc;
    @Autowired private JpaPlayerRepository playerRepository;
    // The same mocks as SecurityTest, so both tests share one Spring context.
    @MockitoBean private SupabaseAuthClient supabaseAuthClient;
    @MockitoBean private JwtDecoder jwtDecoder;

    // Fresh users per test, so tests don't see each other's rosters.
    private final String alice = UUID.randomUUID().toString();
    private final String bob = UUID.randomUUID().toString();

    private static RequestPostProcessor as(String userId) {
        return jwt().jwt(token -> token.subject(userId));
    }

    private Long savePlayer() {
        return playerRepository.save(
            new Player(null, "Ownership Test", NflTeam.BUF, Position.QB, new Workload(60, 240))).getId();
    }

    private long addToRoster(String userId, Long playerId) throws Exception {
        String body = mvc.perform(post("/roster").with(as(userId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"playerId\":" + playerId + ",\"note\":\"Mine\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void eachUserSeesOnlyTheirOwnRoster() throws Exception {
        Long playerId = savePlayer();
        addToRoster(alice, playerId);

        mvc.perform(get("/roster").with(as(alice)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].playerId").value(playerId));
        mvc.perform(get("/roster").with(as(bob)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        // Bob can roster the same player on his own roster.
        addToRoster(bob, playerId);
    }

    @Test
    void anotherUsersEntry_is404_withTheSameErrorAsAnIdThatDoesntExist() throws Exception {
        long alicesEntry = addToRoster(alice, savePlayer());

        mvc.perform(put("/roster/" + alicesEntry).with(as(bob)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"Bob was here\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Roster entry not found: " + alicesEntry));
        mvc.perform(put("/roster/" + alicesEntry + "/slot").with(as(bob)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"slot\":\"QB\"}"))
            .andExpect(status().isNotFound());
        mvc.perform(delete("/roster/" + alicesEntry).with(as(bob)))
            .andExpect(status().isNotFound());
        mvc.perform(delete("/roster/999999").with(as(bob)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Roster entry not found: 999999"));

        // Alice's entry is untouched.
        mvc.perform(get("/roster").with(as(alice)))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].note").value("Mine"))
            .andExpect(jsonPath("$[0].slot").value("BENCH"));
    }

    @Test
    void twoUsersCanChooseDifferentFormats() throws Exception {
        mvc.perform(put("/roster/settings").with(as(alice)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"formatId\":\"yahoo-standard\"}"))
            .andExpect(status().isOk());
        mvc.perform(put("/roster/settings").with(as(bob)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"formatId\":\"sleeper-default\"}"))
            .andExpect(status().isOk());

        mvc.perform(get("/roster/settings").with(as(alice)))
            .andExpect(jsonPath("$.formatId").value("yahoo-standard"));
        mvc.perform(get("/roster/settings").with(as(bob)))
            .andExpect(jsonPath("$.formatId").value("sleeper-default"));
        mvc.perform(get("/roster/settings").with(as(UUID.randomUUID().toString())))
            .andExpect(jsonPath("$.formatId").value("espn-standard"));
    }
}
