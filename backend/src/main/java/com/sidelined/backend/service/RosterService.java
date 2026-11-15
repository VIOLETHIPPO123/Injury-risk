package com.sidelined.backend.service;

import com.sidelined.backend.exception.UnknownRosterFormatException;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.model.RosterFormat;
import com.sidelined.backend.model.RosterSettings;
import com.sidelined.backend.repository.RosterEntryRepository;
import com.sidelined.backend.repository.RosterFormatRepository;
import com.sidelined.backend.repository.RosterSettingsRepository;
import com.sidelined.backend.validation.FormatChangeValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class RosterService {

    private final RosterFormatRepository formatRepository;
    private final RosterSettingsRepository settingsRepository;
    private final RosterEntryRepository rosterEntryRepository;
    // OCP: every rule for switching formats. A new rule is a new class, not an edit here.
    private final List<FormatChangeValidator> formatChangeValidators;

    public RosterService(RosterFormatRepository formatRepository,
                         RosterSettingsRepository settingsRepository,
                         RosterEntryRepository rosterEntryRepository,
                         List<FormatChangeValidator> formatChangeValidators) {
        this.formatRepository = formatRepository;
        this.settingsRepository = settingsRepository;
        this.rosterEntryRepository = rosterEntryRepository;
        this.formatChangeValidators = formatChangeValidators;
    }

    // A table has no order of its own, so sort to keep ESPN, Yahoo, Sleeper.
    public List<RosterFormat> getFormats() {
        return formatRepository.findAll().stream()
            .sorted(Comparator.comparingInt(RosterFormat::displayOrder))
            .toList();
    }

    public RosterSettings getSettings() {
        return settingsRepository.get();
    }

    // The old slots may not exist in the new format, so a switch moves every player to the bench.
    // One transaction: either the bench moves and the new format are both saved, or neither is.
    @Transactional
    public RosterSettings updateSettings(RosterSettings request) {
        String formatId = request.formatId();
        // SRP: a domain exception. ApiExceptionHandler decides it becomes a 400.
        RosterFormat format = Optional.ofNullable(formatId)
            .flatMap(formatRepository::findById)
            .orElseThrow(() -> new UnknownRosterFormatException(formatId));
        formatChangeValidators.forEach(validator -> validator.validate(format));

        for (RosterEntry entry : rosterEntryRepository.findAll()) {
            entry.setSlot(RosterEntry.BENCH);
            rosterEntryRepository.save(entry);
        }
        return settingsRepository.save(new RosterSettings(formatId));
    }
}
