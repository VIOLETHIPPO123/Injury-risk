package com.sidelined.backend.service;

import com.sidelined.backend.exception.PlayerNotFoundException;
import com.sidelined.backend.exception.RosterEntryNotFoundException;
import com.sidelined.backend.model.Player;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.repository.PlayerRepository;
import com.sidelined.backend.repository.RosterEntryRepository;
import com.sidelined.backend.validation.RosterEntryValidator;
import com.sidelined.backend.validation.SlotMoveValidator;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;

// One user's roster. Every method takes the signed-in user's id (their token's "sub") and only
// ever reads or changes that user's entries.
@Service
public class RosterEntryService {

    // DIP: depends on abstractions, not on JPA.
    private final RosterEntryRepository rosterEntryRepository;
    private final PlayerRepository playerRepository;
    private final ProfileService profileService;
    // OCP: every validator Spring finds. A new rule is a new class, not an edit here.
    private final List<RosterEntryValidator> validators;
    private final List<SlotMoveValidator> moveValidators;

    public RosterEntryService(RosterEntryRepository rosterEntryRepository,
                              PlayerRepository playerRepository,
                              ProfileService profileService,
                              List<RosterEntryValidator> validators,
                              List<SlotMoveValidator> moveValidators) {
        this.rosterEntryRepository = rosterEntryRepository;
        this.playerRepository = playerRepository;
        this.profileService = profileService;
        this.validators = validators;
        this.moveValidators = moveValidators;
    }

    // Oldest first, so the roster page keeps a stable order as players are added.
    public List<RosterEntry> getAllEntries(String userId) {
        return rosterEntryRepository.findByProfileId(userId).stream()
            .sorted(Comparator.comparing(RosterEntry::getId))
            .toList();
    }

    public RosterEntry createEntry(String userId, RosterEntry request) {
        validators.forEach(validator -> validator.validate(userId, request));

        // PlayerExistsValidator has already checked the player is there; this fetches it to link to.
        Long playerId = request.getPlayerId();
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new PlayerNotFoundException(playerId));

        // The database assigns ids; ignore any the client sent. New players start on the bench.
        // The owner's profile row has to exist for the foreign key, so a first add creates it.
        return rosterEntryRepository.save(RosterEntry.onRosterOf(
            profileService.getOrCreate(userId), player, RosterEntry.BENCH, request.getNote()));
    }

    public RosterEntry updateEntry(String userId, Long id, RosterEntry changes) {
        // SRP: a domain exception. ApiExceptionHandler decides it becomes a 404.
        RosterEntry existing = findEntry(userId, id);

        // Only the note is editable here; moves go through moveEntry. A missing note keeps the current one.
        if (changes.getNote() != null) {
            existing.setNote(changes.getNote());
        }
        return rosterEntryRepository.save(existing);
    }

    public RosterEntry moveEntry(String userId, Long id, String slot) {
        RosterEntry existing = findEntry(userId, id);
        moveValidators.forEach(validator -> validator.validate(userId, existing, slot));

        existing.setSlot(slot);
        return rosterEntryRepository.save(existing);
    }

    public void deleteEntry(String userId, Long id) {
        rosterEntryRepository.deleteById(findEntry(userId, id).getId());
    }

    // Another user's entry is reported exactly like one that doesn't exist (a 404), so nobody
    // can tell which entry ids are in use on other rosters.
    private RosterEntry findEntry(String userId, Long id) {
        return rosterEntryRepository.findByIdAndProfileId(id, userId)
            .orElseThrow(() -> new RosterEntryNotFoundException(id));
    }
}
