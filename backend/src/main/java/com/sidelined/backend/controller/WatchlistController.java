package com.sidelined.backend.controller;

import com.sidelined.backend.model.WatchlistEntry;
import com.sidelined.backend.service.WatchlistService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping
    public List<WatchlistEntry> getWatchlist() {
        return watchlistService.getAllEntries();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WatchlistEntry addToWatchlist(@RequestBody WatchlistEntry entry) {
        return watchlistService.createEntry(entry);
    }

    @PutMapping("/{id}")
    public WatchlistEntry updateWatchlistEntry(@PathVariable Long id, @RequestBody WatchlistEntry changes) {
        return watchlistService.updateEntry(id, changes);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromWatchlist(@PathVariable Long id) {
        watchlistService.deleteEntry(id);
    }
}
