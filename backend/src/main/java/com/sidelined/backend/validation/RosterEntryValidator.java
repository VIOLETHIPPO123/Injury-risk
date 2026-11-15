package com.sidelined.backend.validation;

import com.sidelined.backend.model.RosterEntry;

// One rule for "can this player be added to this user's roster?". Throw to reject the request.
// Rules about the roster itself (full, already on it) look only at `userId`'s roster.
// Spring injects every @Component that implements this into RosterEntryService, in @Order.
public interface RosterEntryValidator {
    void validate(String userId, RosterEntry request);
}
