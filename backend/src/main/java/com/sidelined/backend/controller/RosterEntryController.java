package com.sidelined.backend.controller;

import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.service.RosterEntryService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

// The players on the signed-in user's roster. Whose roster it is always comes from the token's
// "sub", never from the request. The user's format lives next door in RosterController.
@RestController
@RequestMapping("/roster")
public class RosterEntryController {

    private final RosterEntryService rosterEntryService;

    public RosterEntryController(RosterEntryService rosterEntryService) {
        this.rosterEntryService = rosterEntryService;
    }

    @GetMapping
    public List<RosterEntry> getRoster(@AuthenticationPrincipal Jwt jwt) {
        return rosterEntryService.getAllEntries(jwt.getSubject());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RosterEntry addToRoster(@AuthenticationPrincipal Jwt jwt, @RequestBody RosterEntry entry) {
        return rosterEntryService.createEntry(jwt.getSubject(), entry);
    }

    @PutMapping("/{id}")
    public RosterEntry updateRosterEntry(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                         @RequestBody RosterEntry changes) {
        return rosterEntryService.updateEntry(jwt.getSubject(), id, changes);
    }

    // Body: { "slot": "RB" }, or BENCH or IR.
    @PutMapping("/{id}/slot")
    public RosterEntry moveRosterEntry(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                       @RequestBody RosterEntry move) {
        return rosterEntryService.moveEntry(jwt.getSubject(), id, move.getSlot());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromRoster(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        rosterEntryService.deleteEntry(jwt.getSubject(), id);
    }
}
