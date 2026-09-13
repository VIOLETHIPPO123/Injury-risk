package com.sidelined.backend.controller;

import com.sidelined.backend.model.Team;
import com.sidelined.backend.service.TeamService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping("/teams")
    public List<Team> getAllTeams() {
        return teamService.getAllTeams();
    }
}