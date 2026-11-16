package com.sidelined.backend.validation;

import com.sidelined.backend.model.RosterEntry;

// One rule for "can this player move to that slot?". Throw to reject the move.
// `entry` is the player as they are now, on `userId`'s roster; `slot` is where they'd go.
// Room and slots are counted on that user's roster and format only.
// Spring injects every @Component that implements this into RosterEntryService, in @Order.
public interface SlotMoveValidator {
    void validate(String userId, RosterEntry entry, String slot);
}
