package com.sidelined.backend.validation;

import com.sidelined.backend.model.RosterFormat;

// One rule for "can the league switch to this format?". Throw to reject the switch.
// Spring injects every @Component that implements this into RosterService.
public interface FormatChangeValidator {
    void validate(RosterFormat format);
}
