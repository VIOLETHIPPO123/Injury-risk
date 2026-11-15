package com.sidelined.backend.controller;

import com.sidelined.backend.model.RosterFormat;
import com.sidelined.backend.model.RosterSettings;
import com.sidelined.backend.service.RosterService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/roster")
public class RosterController {

    private final RosterService rosterService;

    public RosterController(RosterService rosterService) {
        this.rosterService = rosterService;
    }

    // The same for everyone, and public (SecurityConfig).
    @GetMapping("/formats")
    public List<RosterFormat> getFormats() {
        return rosterService.getFormats();
    }

    // The signed-in user's format.
    @GetMapping("/settings")
    public RosterSettings getSettings(@AuthenticationPrincipal Jwt jwt) {
        return rosterService.getSettings(jwt.getSubject());
    }

    @PutMapping("/settings")
    public RosterSettings updateSettings(@AuthenticationPrincipal Jwt jwt, @RequestBody RosterSettings settings) {
        return rosterService.updateSettings(jwt.getSubject(), settings);
    }
}
