package com.sidelined.backend.service;

import com.sidelined.backend.exception.RosterEntryNotFoundException;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.repository.RosterEntryRepository;
import com.sidelined.backend.validation.RosterEntryValidator;
import com.sidelined.backend.validation.SlotMoveValidator;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;

@Service
public class RosterEntryService {

    // DIP: depends on abstractions. Swapping in a database never touches this class.
    private final RosterEntryRepository rosterEntryRepository;
    // OCP: every validator Spring finds. A new rule is a new class, not an edit here.
    private final List<RosterEntryValidator> validators;
    private final List<SlotMoveValidator> moveValidators;

    public RosterEntryService(RosterEntryRepository rosterEntryRepository,
                              List<RosterEntryValidator> validators,
                              List<SlotMoveValidator> moveValidators) {
        this.rosterEntryRepository = rosterEntryRepository;
        this.validators = validators;
        this.moveValidators = moveValidators;
    }

    // Oldest first, so the roster page keeps a stable order as players are added.
    public List<RosterEntry> getAllEntries() {
        return rosterEntryRepository.findAll().stream()
            .sorted(Comparator.comparing(RosterEntry::getId))
            .toList();
    }

    public RosterEntry createEntry(RosterEntry request) {
        validators.forEach(validator -> validator.validate(request));

        // The repository assigns ids; ignore any the client sent. New players start on the bench.
        RosterEntry entry = new RosterEntry(null, request.getPlayerId(), RosterEntry.BENCH, request.getNote());
        return rosterEntryRepository.save(entry);
    }

    public RosterEntry updateEntry(Long id, RosterEntry changes) {
        // SRP: a domain exception. ApiExceptionHandler decides it becomes a 404.
        RosterEntry existing = findEntry(id);

        // Only the note is editable here; moves go through moveEntry. A missing note keeps the current one.
        if (changes.getNote() != null) {
            existing.setNote(changes.getNote());
        }
        return rosterEntryRepository.save(existing);
    }

    public RosterEntry moveEntry(Long id, String slot) {
        RosterEntry existing = findEntry(id);
        moveValidators.forEach(validator -> validator.validate(existing, slot));

        existing.setSlot(slot);
        return rosterEntryRepository.save(existing);
    }

    public void deleteEntry(Long id) {
        if (!rosterEntryRepository.existsById(id)) {
            throw new RosterEntryNotFoundException(id);
        }
        rosterEntryRepository.deleteById(id);
    }

    private RosterEntry findEntry(Long id) {
        return rosterEntryRepository.findById(id)
            .orElseThrow(() -> new RosterEntryNotFoundException(id));
    }
}
