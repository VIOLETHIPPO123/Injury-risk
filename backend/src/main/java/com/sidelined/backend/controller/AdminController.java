package com.sidelined.backend.controller;

import com.sidelined.backend.service.PlayerSyncJob;
import com.sidelined.backend.service.ProfileService;
import com.sidelined.backend.service.WorkloadSyncJob;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Endpoints only an ADMIN may call. SecurityConfig turns away a request with no token (401);
// each method here then checks the role itself (403), so hiding a button in the frontend is
// never what keeps a non-admin out.
@RestController
@RequestMapping("/admin")
public class AdminController {

    // POST /admin/players/refresh's body: what each job did.
    public record RefreshResponse(PlayerSyncJob.Result players, WorkloadSyncJob.Result workloads) {
    }

    private final ProfileService profileService;
    private final PlayerSyncJob playerSyncJob;
    private final WorkloadSyncJob workloadSyncJob;

    public AdminController(ProfileService profileService, PlayerSyncJob playerSyncJob,
                           WorkloadSyncJob workloadSyncJob) {
        this.profileService = profileService;
        this.playerSyncJob = playerSyncJob;
        this.workloadSyncJob = workloadSyncJob;
    }

    // Runs Tuesday's two jobs now, in the same order: players first, because snaps can only be
    // stored for players that exist. It answers when both have finished, which takes a few
    // seconds. If nflverse can't be reached it is a 502 (via ApiExceptionHandler).
    @PostMapping("/players/refresh")
    public RefreshResponse refreshPlayers(@AuthenticationPrincipal Jwt jwt) {
        profileService.requireAdmin(jwt.getSubject());
        PlayerSyncJob.Result players = playerSyncJob.sync();
        WorkloadSyncJob.Result workloads = workloadSyncJob.sync();
        return new RefreshResponse(players, workloads);
    }
}
