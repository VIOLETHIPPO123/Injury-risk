package com.sidelined.backend.validation;

import com.sidelined.backend.exception.RosterFullException;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.model.RosterFormat;
import com.sidelined.backend.repository.RosterEntryRepository;
import com.sidelined.backend.service.CurrentRosterFormat;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// Rule: no adding past the maximum of the user's format. Players on IR don't count toward it,
// and neither does anyone on another user's roster.
@Component
@Order(4)
public class RosterNotFullValidator implements RosterEntryValidator {

    private final RosterEntryRepository rosterEntryRepository;
    private final CurrentRosterFormat currentFormat;

    public RosterNotFullValidator(RosterEntryRepository rosterEntryRepository,
                                  CurrentRosterFormat currentFormat) {
        this.rosterEntryRepository = rosterEntryRepository;
        this.currentFormat = currentFormat;
    }

    @Override
    public void validate(String userId, RosterEntry request) {
        RosterFormat format = currentFormat.get(userId);
        long counted = rosterEntryRepository.findByProfileId(userId).stream()
            .filter(e -> !RosterEntry.IR.equals(e.getSlot()))
            .count();
        if (counted >= format.maxRosterSize()) {
            throw new RosterFullException(format.maxRosterSize());
        }
    }
}
