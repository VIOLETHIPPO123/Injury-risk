package com.sidelined.backend.controller;

import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Player;
import com.sidelined.backend.model.Position;
import com.sidelined.backend.model.Profile;
import com.sidelined.backend.model.Role;
import com.sidelined.backend.model.Workload;
import com.sidelined.backend.model.Credentials;
import com.sidelined.backend.repository.JpaPlayerRepository;
import com.sidelined.backend.repository.ProfileRepository;
import com.sidelined.backend.service.ProfileService;
import com.sidelined.backend.service.SupabaseAuthClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// The real security setup (SecurityConfig) on the whole app, with an in-memory H2 database.
// jwt() stands in for a verified Supabase token, so no request reaches Supabase.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecurityTest.AdminOnlyController.class)
class SecurityTest {

    // An endpoint that only uses the shared admin check, so the check can be tested over HTTP
    // before any real admin endpoint exists (DEV-104).
    @RestController
    static class AdminOnlyController {
        private final ProfileService profileService;

        AdminOnlyController(ProfileService profileService) {
            this.profileService = profileService;
        }

        @GetMapping("/test/admin-only")
        String adminOnly(@AuthenticationPrincipal Jwt jwt) {
            profileService.requireAdmin(jwt.getSubject());
            return "ok";
        }
    }

    @Autowired private MockMvc mvc;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private JpaPlayerRepository playerRepository;

    @MockitoBean private SupabaseAuthClient supabaseAuthClient;
    // Only reached by a raw "Bearer ..." header; jwt() skips decoding.
    @MockitoBean private JwtDecoder jwtDecoder;

    // A fresh user per test, so tests don't see each other's profiles.
    private static String newUserId() {
        return UUID.randomUUID().toString();
    }

    private static RequestPostProcessor tokenFor(String userId) {
        return jwt().jwt(token -> token.subject(userId));
    }

    // ---------- Public endpoints: no token needed ----------

    @Test
    void publicGets_workWithoutAToken() throws Exception {
        Long playerId = playerRepository.save(
            new Player(null, "Public Test", NflTeam.ARI, Position.QB, new Workload(10, 40))).getId();

        mvc.perform(get("/players")).andExpect(status().isOk());
        mvc.perform(get("/teams")).andExpect(status().isOk());
        mvc.perform(get("/roster/formats")).andExpect(status().isOk());
        mvc.perform(get("/players/" + playerId + "/injuries")).andExpect(status().isOk());
        mvc.perform(get("/players/" + playerId + "/workload")).andExpect(status().isOk());
    }

    @Test
    void publicGet_errorKeepsItsStatus_insteadOf401() throws Exception {
        // An unknown player is still a 404 for a visitor with no token.
        mvc.perform(get("/players/999999/injuries")).andExpect(status().isNotFound());
    }

    @Test
    void signIn_isPublic_andReturnsSupabasesResponse() throws Exception {
        when(supabaseAuthClient.signIn(any(Credentials.class)))
            .thenReturn(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body("{\"access_token\":\"abc\"}"));

        mvc.perform(post("/auth/signin").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"fan@example.com\",\"password\":\"secret123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access_token").value("abc"));

        verify(supabaseAuthClient).signIn(new Credentials("fan@example.com", "secret123"));
    }

    @Test
    void signUp_isPublic_andPassesSupabaseErrorsThrough() throws Exception {
        when(supabaseAuthClient.signUp(any(Credentials.class)))
            .thenReturn(ResponseEntity.unprocessableContent().contentType(MediaType.APPLICATION_JSON)
                .body("{\"msg\":\"User already registered\"}"));

        mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"fan@example.com\",\"password\":\"secret123\"}"))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.msg").value("User already registered"));
    }

    // ---------- Protected endpoints: 401 without a valid token ----------

    @Test
    void protectedEndpoints_withoutAToken_return401() throws Exception {
        mvc.perform(get("/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/roster")).andExpect(status().isUnauthorized());
        mvc.perform(post("/roster").contentType(MediaType.APPLICATION_JSON).content("{\"playerId\":1}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(put("/roster/settings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"formatId\":\"espn-standard\"}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(delete("/roster/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withAnInvalidToken_returns401() throws Exception {
        when(jwtDecoder.decode("not-a-real-token")).thenThrow(new BadJwtException("bad signature"));

        mvc.perform(get("/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withAValidToken_isAllowed() throws Exception {
        mvc.perform(get("/roster").with(tokenFor(newUserId()))).andExpect(status().isOk());
    }

    // ---------- /me ----------

    @Test
    void me_firstRequest_createsAUserProfile_andLaterRequestsReuseIt() throws Exception {
        String userId = newUserId();
        long before = profileRepository.count();

        mvc.perform(get("/me").with(tokenFor(userId)))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"role\":\"USER\"}", true));
        mvc.perform(get("/me").with(tokenFor(userId)))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"role\":\"USER\"}", true));

        assertEquals(before + 1, profileRepository.count());
        assertEquals(Role.USER, profileRepository.findById(userId).orElseThrow().getRole());
    }

    @Test
    void me_admin_returnsAdmin() throws Exception {
        String userId = newUserId();
        profileRepository.save(new Profile(userId, Role.ADMIN));

        mvc.perform(get("/me").with(tokenFor(userId)))
            .andExpect(content().json("{\"role\":\"ADMIN\"}", true));
    }

    // ---------- The admin check ----------

    @Test
    void adminCheck_user_gets403() throws Exception {
        String userId = newUserId();
        profileRepository.save(new Profile(userId, Role.USER));

        mvc.perform(get("/test/admin-only").with(tokenFor(userId)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("Admins only"));
    }

    @Test
    void adminCheck_noProfileYet_gets403() throws Exception {
        mvc.perform(get("/test/admin-only").with(tokenFor(newUserId())))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCheck_admin_isLetThrough() throws Exception {
        String userId = newUserId();
        profileRepository.save(new Profile(userId, Role.ADMIN));

        mvc.perform(get("/test/admin-only").with(tokenFor(userId)))
            .andExpect(status().isOk())
            .andExpect(content().string("ok"));
    }

    @Test
    void adminCheck_withoutAToken_gets401() throws Exception {
        mvc.perform(get("/test/admin-only")).andExpect(status().isUnauthorized());
    }

    // ---------- CORS ----------

    @Test
    void corsPreflight_fromTheFrontend_allowsTheAuthorizationHeader() throws Exception {
        mvc.perform(options("/roster")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void corsPreflight_fromAnotherSite_isRejected() throws Exception {
        mvc.perform(options("/roster")
                .header(HttpHeaders.ORIGIN, "https://evil.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .andExpect(status().isForbidden());
    }
}
