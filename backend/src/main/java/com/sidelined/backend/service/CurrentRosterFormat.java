package com.sidelined.backend.service;

import com.sidelined.backend.exception.UnknownRosterFormatException;
import com.sidelined.backend.model.RosterFormat;
import com.sidelined.backend.repository.RosterFormatRepository;
import com.sidelined.backend.repository.RosterSettingsRepository;
import org.springframework.stereotype.Component;

// The format the league is on now. The roster rules all check against it.
@Component
public class CurrentRosterFormat {

    private final RosterFormatRepository formatRepository;
    private final RosterSettingsRepository settingsRepository;

    public CurrentRosterFormat(RosterFormatRepository formatRepository,
                               RosterSettingsRepository settingsRepository) {
        this.formatRepository = formatRepository;
        this.settingsRepository = settingsRepository;
    }

    public RosterFormat get() {
        String formatId = settingsRepository.get().formatId();
        return formatRepository.findById(formatId)
            .orElseThrow(() -> new UnknownRosterFormatException(formatId));
    }
}
