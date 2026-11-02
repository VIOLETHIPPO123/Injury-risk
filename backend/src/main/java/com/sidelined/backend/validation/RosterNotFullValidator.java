package com.sidelined.backend.validation;

import com.sidelined.backend.exception.RosterFullException;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.model.RosterFormat;
import com.sidelined.backend.repository.RosterEntryRepository;
import com.sidelined.backend.service.CurrentRosterFormat;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// Rule: no adding past the format's maximum. Players on IR don't count toward it.
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
    public void validate(RosterEntry request) {
        RosterFormat format = currentFormat.get();
        long counted = rosterEntryRepository.findAll().stream()
            .filter(e -> !RosterEntry.IR.equals(e.getSlot()))
            .count();
        if (counted >= format.maxRosterSize()) {
            throw new RosterFullException(format.maxRosterSize());
        }
    }
}
