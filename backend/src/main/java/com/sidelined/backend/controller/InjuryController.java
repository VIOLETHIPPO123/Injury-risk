package com.sidelined.backend.controller;

import com.sidelined.backend.model.Injury;
import com.sidelined.backend.service.InjuryService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class InjuryController {

    private final InjuryService injuryService;

    public InjuryController(InjuryService injuryService) {
        this.injuryService = injuryService;
    }

    // A player's injury history, newest first.
    @GetMapping("/players/{playerId}/injuries")
    public List<Injury> getInjuries(@PathVariable Long playerId) {
        return injuryService.getInjuries(playerId);
    }
}
