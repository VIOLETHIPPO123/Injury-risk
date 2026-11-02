package com.sidelined.backend.validation;

import com.sidelined.backend.model.RosterEntry;

// One rule for "can this player be added?". Throw to reject the request.
// Spring injects every @Component that implements this into RosterEntryService, in @Order.
public interface RosterEntryValidator {
    void validate(RosterEntry request);
}
