package com.sidelined.backend.controller;

import com.sidelined.backend.exception.NflverseUnavailableException;
import com.sidelined.backend.model.Profile;
import com.sidelined.backend.model.Role;
import com.sidelined.backend.repository.ProfileRepository;
import com.sidelined.backend.service.PlayerSyncJob;
import com.sidelined.backend.service.WorkloadSyncJob;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// POST /admin/players/refresh over real HTTP: the whole app, the real security filters and the
// H2 database, with the two sync jobs mocked so nothing is downloaded. jwt() stands in for a
// verified Supabase token; the role comes from the user's row in profiles, as it does for real.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminControllerTest {

    private static final String REFRESH = "/admin/players/refresh";

    @Autowired private MockMvc mvc;
    @Autowired private ProfileRepository profileRepository;

    @MockitoBean private PlayerSyncJob playerSyncJob;
    @MockitoBean private WorkloadSyncJob workloadSyncJob;
    // Only reached by a raw "Bearer ..." header; jwt() skips decoding.
    @MockitoBean private JwtDecoder jwtDecoder;

    // A fresh user per test, stored with the given role.
    private RequestPostProcessor tokenForA(Role role) {
        String userId = UUID.randomUUID().toString();
        profileRepository.save(new Profile(userId, role));
        return jwt().jwt(token -> token.subject(userId));
    }

    // ---------- Admin: 200, and both jobs run ----------

    @Test
    void admin_gets200_andBothJobsRun_playersFirst() throws Exception {
        // Arrange
        when(playerSyncJob.sync()).thenReturn(new PlayerSyncJob.Result(3, 12, 1, 0));
        when(workloadSyncJob.sync()).thenReturn(new WorkloadSyncJob.Result(1400, 2, 60));

        // Act + Assert
        mvc.perform(post(REFRESH).with(tokenForA(Role.ADMIN)))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"players\":{\"added\":3,\"updated\":12,\"skipped\":1,\"defensesAdded\":0},"
                + "\"workloads\":{\"added\":1400,\"updated\":2,\"unmatched\":60}}"));

        InOrder order = inOrder(playerSyncJob, workloadSyncJob);
        order.verify(playerSyncJob).sync();
        order.verify(workloadSyncJob).sync();
    }

    @Test
    void admin_whenNflverseIsDown_gets502WithTheReason() throws Exception {
        when(playerSyncJob.sync()).thenThrow(new NflverseUnavailableException("Couldn't download players"));

        mvc.perform(post(REFRESH).with(tokenForA(Role.ADMIN)))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.error").value("Couldn't download players"));

        verify(workloadSyncJob, never()).sync();
    }

    // ---------- Signed in but not an admin: 403 ----------

    @Test
    void nonAdmin_gets403_andNeitherJobRuns() throws Exception {
        // This is the endpoint called directly with a USER's token: no button involved.
        mvc.perform(post(REFRESH).with(tokenForA(Role.USER)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("Admins only"));

        verifyNoInteractions(playerSyncJob, workloadSyncJob);
    }

    @Test
    void signedInUserWithNoProfileYet_gets403() throws Exception {
        // Someone whose first request ever is this one.
        mvc.perform(post(REFRESH).with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))))
            .andExpect(status().isForbidden());

        verifyNoInteractions(playerSyncJob, workloadSyncJob);
    }

    // ---------- No token: 401 ----------

    @Test
    void noToken_gets401_andNeitherJobRuns() throws Exception {
        mvc.perform(post(REFRESH))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(playerSyncJob, workloadSyncJob);
    }

    @Test
    void invalidToken_gets401() throws Exception {
        when(jwtDecoder.decode("not-a-real-token")).thenThrow(new BadJwtException("bad signature"));

        mvc.perform(post(REFRESH).header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(playerSyncJob, workloadSyncJob);
    }

    // ---------- Only POST ----------

    @Test
    void get_isNotAllowed_soALinkOrPrefetchCantTriggerARefresh() throws Exception {
        mvc.perform(get(REFRESH).with(tokenForA(Role.ADMIN)))
            .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(playerSyncJob, workloadSyncJob);
    }
}
