package com.sidelined.backend.service;

import com.sidelined.backend.exception.UnknownRosterFormatException;
import com.sidelined.backend.model.RosterFormat;
import com.sidelined.backend.model.RosterSettings;
import com.sidelined.backend.repository.RosterFormatRepository;
import com.sidelined.backend.repository.RosterSettingsRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RosterService {

    private final RosterFormatRepository formatRepository;
    private final RosterSettingsRepository settingsRepository;

    public RosterService(RosterFormatRepository formatRepository,
                         RosterSettingsRepository settingsRepository) {
        this.formatRepository = formatRepository;
        this.settingsRepository = settingsRepository;
    }

    public List<RosterFormat> getFormats() {
        return formatRepository.findAll();
    }

    public RosterSettings getSettings() {
        return settingsRepository.get();
    }

    public RosterSettings updateSettings(RosterSettings request) {
        String formatId = request.formatId();
        // SRP: a domain exception. ApiExceptionHandler decides it becomes a 400.
        if (formatId == null || !formatRepository.existsById(formatId)) {
            throw new UnknownRosterFormatException(formatId);
        }
        return settingsRepository.save(new RosterSettings(formatId));
    }
}
