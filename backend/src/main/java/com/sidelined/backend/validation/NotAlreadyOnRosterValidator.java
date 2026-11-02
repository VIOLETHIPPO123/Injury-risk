package com.sidelined.backend.validation;

import com.sidelined.backend.exception.PlayerAlreadyOnRosterException;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.repository.RosterEntryRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// Rule: a player can be on the roster only once.
@Component
@Order(3)
public class NotAlreadyOnRosterValidator implements RosterEntryValidator {

    private final RosterEntryRepository rosterEntryRepository;

    public NotAlreadyOnRosterValidator(RosterEntryRepository rosterEntryRepository) {
        this.rosterEntryRepository = rosterEntryRepository;
    }

    @Override
    public void validate(RosterEntry request) {
        boolean onRoster = rosterEntryRepository.findAll().stream()
            .anyMatch(e -> e.getPlayerId().equals(request.getPlayerId()));
        if (onRoster) {
            throw new PlayerAlreadyOnRosterException(request.getPlayerId());
        }
    }
}
