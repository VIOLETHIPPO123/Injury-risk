package com.sidelined.backend.validation;

import com.sidelined.backend.model.RosterFormat;

// One rule for "can this user's roster switch to this format?". Throw to reject the switch.
// Spring injects every @Component that implements this into RosterService.
public interface FormatChangeValidator {
    void validate(String userId, RosterFormat format);
}
