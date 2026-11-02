package com.sidelined.backend.validation;

import com.sidelined.backend.exception.SlotFullException;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.model.RosterFormat;
import com.sidelined.backend.model.RosterSlot;
import com.sidelined.backend.repository.RosterEntryRepository;
import com.sidelined.backend.service.CurrentRosterFormat;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// Rule: a slot holds at most its count (RB holds 2, IR holds the format's irCount).
// The bench has no limit of its own; the roster maximum caps it. A player already in the
// slot doesn't take up the room they'd be moving into.
@Component
@Order(2)
public class SlotHasRoomValidator implements SlotMoveValidator {

    private final RosterEntryRepository rosterEntryRepository;
    private final CurrentRosterFormat currentFormat;

    public SlotHasRoomValidator(RosterEntryRepository rosterEntryRepository,
                                CurrentRosterFormat currentFormat) {
        this.rosterEntryRepository = rosterEntryRepository;
        this.currentFormat = currentFormat;
    }

    @Override
    public void validate(RosterEntry entry, String slot) {
        if (RosterEntry.BENCH.equals(slot)) {
            return;
        }
        RosterFormat format = currentFormat.get();
        // SlotAllowsPositionValidator has already turned away slots the format doesn't have.
        int room = RosterEntry.IR.equals(slot)
            ? format.irCount()
            : format.findSlot(slot).map(RosterSlot::count).orElse(0);

        long taken = rosterEntryRepository.findAll().stream()
            .filter(e -> slot.equals(e.getSlot()) && !e.getId().equals(entry.getId()))
            .count();
        if (taken >= room) {
            throw new SlotFullException(slot);
        }
    }
}
