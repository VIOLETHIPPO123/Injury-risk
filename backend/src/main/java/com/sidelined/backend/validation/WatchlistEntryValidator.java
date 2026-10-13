package com.sidelined.backend.validation;

import com.sidelined.backend.model.WatchlistEntry;

// One rule for "can this entry be added?". Throw to reject the request.
// Spring injects every @Component that implements this into WatchlistService.
public interface WatchlistEntryValidator {
    void validate(WatchlistEntry request);
}
