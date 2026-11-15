package com.sidelined.backend.validation;

import com.sidelined.backend.exception.PlayerAlreadyOnRosterException;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.repository.RosterEntryRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// Rule: a player can be on a user's roster only once. Other users can roster them too.
@Component
@Order(3)
public class NotAlreadyOnRosterValidator implements RosterEntryValidator {

    private final RosterEntryRepository rosterEntryRepository;

    public NotAlreadyOnRosterValidator(RosterEntryRepository rosterEntryRepository) {
        this.rosterEntryRepository = rosterEntryRepository;
    }

    @Override
    public void validate(String userId, RosterEntry request) {
        boolean onRoster = rosterEntryRepository.findByProfileId(userId).stream()
            .anyMatch(e -> e.getPlayerId().equals(request.getPlayerId()));
        if (onRoster) {
            throw new PlayerAlreadyOnRosterException(request.getPlayerId());
        }
    }
}
