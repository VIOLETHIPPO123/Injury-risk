package com.sidelined.backend.controller;

import com.sidelined.backend.model.RosterFormat;
import com.sidelined.backend.model.RosterSettings;
import com.sidelined.backend.service.RosterService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/roster")
public class RosterController {

    private final RosterService rosterService;

    public RosterController(RosterService rosterService) {
        this.rosterService = rosterService;
    }

    @GetMapping("/formats")
    public List<RosterFormat> getFormats() {
        return rosterService.getFormats();
    }

    @GetMapping("/settings")
    public RosterSettings getSettings() {
        return rosterService.getSettings();
    }

    @PutMapping("/settings")
    public RosterSettings updateSettings(@RequestBody RosterSettings settings) {
        return rosterService.updateSettings(settings);
    }
}
