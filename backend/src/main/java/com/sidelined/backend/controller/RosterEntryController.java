package com.sidelined.backend.controller;

import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.service.RosterEntryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

// The players on the roster. The league's format lives next door in RosterController.
@RestController
@RequestMapping("/roster")
public class RosterEntryController {

    private final RosterEntryService rosterEntryService;

    public RosterEntryController(RosterEntryService rosterEntryService) {
        this.rosterEntryService = rosterEntryService;
    }

    @GetMapping
    public List<RosterEntry> getRoster() {
        return rosterEntryService.getAllEntries();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RosterEntry addToRoster(@RequestBody RosterEntry entry) {
        return rosterEntryService.createEntry(entry);
    }

    @PutMapping("/{id}")
    public RosterEntry updateRosterEntry(@PathVariable Long id, @RequestBody RosterEntry changes) {
        return rosterEntryService.updateEntry(id, changes);
    }

    // Body: { "slot": "RB" }, or BENCH or IR.
    @PutMapping("/{id}/slot")
    public RosterEntry moveRosterEntry(@PathVariable Long id, @RequestBody RosterEntry move) {
        return rosterEntryService.moveEntry(id, move.getSlot());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromRoster(@PathVariable Long id) {
        rosterEntryService.deleteEntry(id);
    }
}
