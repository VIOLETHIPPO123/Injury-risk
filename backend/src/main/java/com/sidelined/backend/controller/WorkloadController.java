package com.sidelined.backend.controller;

import com.sidelined.backend.model.WeeklySnaps;
import com.sidelined.backend.service.WorkloadService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class WorkloadController {

    private final WorkloadService workloadService;

    public WorkloadController(WorkloadService workloadService) {
        this.workloadService = workloadService;
    }

    // A player's snaps for each week of the season so far, oldest first; a bye week has
    // "snaps": null.
    @GetMapping("/players/{playerId}/workload")
    public List<WeeklySnaps> getWorkload(@PathVariable Long playerId) {
        return workloadService.getWorkload(playerId);
    }
}
