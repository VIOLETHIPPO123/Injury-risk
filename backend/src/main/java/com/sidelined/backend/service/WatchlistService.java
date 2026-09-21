package com.sidelined.backend.service;

import com.sidelined.backend.model.WatchlistEntry;
import com.sidelined.backend.repository.WatchlistRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final PlayerService playerService;

    public WatchlistService(WatchlistRepository watchlistRepository, PlayerService playerService) {
        this.watchlistRepository = watchlistRepository;
        this.playerService = playerService;
    }

    public WatchlistEntry createEntry(WatchlistEntry request) {
        boolean playerExists = playerService.getAllPlayers().stream()
            .anyMatch(player -> player.getId().equals(request.getPlayerId()));
        if (!playerExists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found");
        }

        // The repository assigns ids; ignore any the client sent.
        WatchlistEntry entry = new WatchlistEntry(null, request.getPlayerId(), request.getNote());
        return watchlistRepository.save(entry);
    }

    public WatchlistEntry updateEntry(Long id, WatchlistEntry changes) {
        WatchlistEntry existing = watchlistRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Watchlist entry not found"));

        // Only the note is editable; playerId stays fixed. A missing note keeps the current one.
        if (changes.getNote() != null) {
            existing.setNote(changes.getNote());
        }
        return watchlistRepository.save(existing);
    }
}
