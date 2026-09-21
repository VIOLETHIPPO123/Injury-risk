package com.sidelined.backend.controller;

import com.sidelined.backend.model.WatchlistEntry;
import com.sidelined.backend.service.WatchlistService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
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
}
