package com.sidelined.backend.controller;

import com.sidelined.backend.model.Role;
import com.sidelined.backend.service.ProfileService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    // GET /me's body: { "role": "USER" } or { "role": "ADMIN" }.
    public record MeResponse(Role role) {
    }

    private final ProfileService profileService;

    public MeController(ProfileService profileService) {
        this.profileService = profileService;
    }

    // The signed-in user's role. Their first request creates their profile.
    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        return new MeResponse(profileService.getOrCreate(jwt.getSubject()).getRole());
    }
}
