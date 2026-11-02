package com.sidelined.backend.validation;

import com.sidelined.backend.model.RosterEntry;

// One rule for "can this player move to that slot?". Throw to reject the move.
// `entry` is the player as they are now; `slot` is where they'd go.
// Spring injects every @Component that implements this into RosterEntryService, in @Order.
public interface SlotMoveValidator {
    void validate(RosterEntry entry, String slot);
}
