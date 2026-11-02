package com.sidelined.backend.validation;

import com.sidelined.backend.exception.RosterTooBigForFormatException;
import com.sidelined.backend.model.RosterFormat;
import com.sidelined.backend.repository.RosterEntryRepository;
import org.springframework.stereotype.Component;

// Rule: a switch moves everyone to the bench, IR included, so the whole roster has to
// fit under the new format's maximum.
@Component
public class RosterFitsFormatValidator implements FormatChangeValidator {

    private final RosterEntryRepository rosterEntryRepository;

    public RosterFitsFormatValidator(RosterEntryRepository rosterEntryRepository) {
        this.rosterEntryRepository = rosterEntryRepository;
    }

    @Override
    public void validate(RosterFormat format) {
        int size = rosterEntryRepository.findAll().size();
        if (size > format.maxRosterSize()) {
            throw new RosterTooBigForFormatException(format.name(), format.maxRosterSize(), size);
        }
    }
}
