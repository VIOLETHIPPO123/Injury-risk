package com.sidelined.backend.controller;

import com.sidelined.backend.model.WorkloadEntry;
import com.sidelined.backend.service.WorkloadService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class WorkloadController {

    private final WorkloadService workloadService;

    public WorkloadController(WorkloadService workloadService) {
        this.workloadService = workloadService;
    }

    // A player's games so far, in week order. A bye week has no entry.
    @GetMapping("/players/{playerId}/workload")
    public List<WorkloadEntry> getWorkload(@PathVariable Long playerId) {
        return workloadService.getWorkload(playerId);
    }
}
