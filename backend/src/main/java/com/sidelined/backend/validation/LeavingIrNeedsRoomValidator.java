package com.sidelined.backend.validation;

import com.sidelined.backend.exception.RosterFullException;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.model.RosterFormat;
import com.sidelined.backend.repository.RosterEntryRepository;
import com.sidelined.backend.service.CurrentRosterFormat;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// Rule: IR doesn't count toward the roster maximum, so bringing a player back off IR
// needs a free spot, just as adding a player does.
@Component
@Order(4)
public class LeavingIrNeedsRoomValidator implements SlotMoveValidator {

    private final RosterEntryRepository rosterEntryRepository;
    private final CurrentRosterFormat currentFormat;

    public LeavingIrNeedsRoomValidator(RosterEntryRepository rosterEntryRepository,
                                       CurrentRosterFormat currentFormat) {
        this.rosterEntryRepository = rosterEntryRepository;
        this.currentFormat = currentFormat;
    }

    @Override
    public void validate(RosterEntry entry, String slot) {
        if (!RosterEntry.IR.equals(entry.getSlot()) || RosterEntry.IR.equals(slot)) {
            return;
        }
        RosterFormat format = currentFormat.get();
        long counted = rosterEntryRepository.findAll().stream()
            .filter(e -> !RosterEntry.IR.equals(e.getSlot()))
            .count();
        if (counted >= format.maxRosterSize()) {
            throw new RosterFullException(format.maxRosterSize());
        }
    }
}
