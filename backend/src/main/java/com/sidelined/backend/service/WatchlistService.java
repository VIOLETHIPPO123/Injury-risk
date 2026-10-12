package com.sidelined.backend.service;

import com.sidelined.backend.exception.WatchlistEntryNotFoundException;
import com.sidelined.backend.model.WatchlistEntry;
import com.sidelined.backend.repository.WatchlistRepository;
import com.sidelined.backend.validation.WatchlistEntryValidator;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;

@Service
public class WatchlistService {

    // DIP: depends on abstractions. Swapping in a database never touches this class.
    private final WatchlistRepository watchlistRepository;
    // OCP: every validator Spring finds. A new rule is a new class, not an edit here.
    private final List<WatchlistEntryValidator> validators;

    public WatchlistService(WatchlistRepository watchlistRepository,
                            List<WatchlistEntryValidator> validators) {
        this.watchlistRepository = watchlistRepository;
        this.validators = validators;
    }

    // Oldest first, so the watchlist page keeps a stable order as entries are added.
    public List<WatchlistEntry> getAllEntries() {
        return watchlistRepository.findAll().stream()
            .sorted(Comparator.comparing(WatchlistEntry::getId))
            .toList();
    }

    public WatchlistEntry createEntry(WatchlistEntry request) {
        validators.forEach(validator -> validator.validate(request));

        // The repository assigns ids; ignore any the client sent.
        WatchlistEntry entry = new WatchlistEntry(null, request.getPlayerId(), request.getNote());
        return watchlistRepository.save(entry);
    }

    public WatchlistEntry updateEntry(Long id, WatchlistEntry changes) {
        // SRP: a domain exception. ApiExceptionHandler decides it becomes a 404.
        WatchlistEntry existing = watchlistRepository.findById(id)
            .orElseThrow(() -> new WatchlistEntryNotFoundException(id));

        // Only the note is editable; playerId stays fixed. A missing note keeps the current one.
        if (changes.getNote() != null) {
            existing.setNote(changes.getNote());
        }
        return watchlistRepository.save(existing);
    }

    public void deleteEntry(Long id) {
        if (!watchlistRepository.deleteById(id)) {
            throw new WatchlistEntryNotFoundException(id);
        }
    }
}
