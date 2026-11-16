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

// The formats (the same for everyone) and each user's choice of one.
@Service
public class RosterService {

    private final RosterFormatRepository formatRepository;
    private final RosterSettingsRepository settingsRepository;
    private final RosterEntryRepository rosterEntryRepository;
    private final ProfileService profileService;
    // OCP: every rule for switching formats. A new rule is a new class, not an edit here.
    private final List<FormatChangeValidator> formatChangeValidators;

    public RosterService(RosterFormatRepository formatRepository,
                         RosterSettingsRepository settingsRepository,
                         RosterEntryRepository rosterEntryRepository,
                         ProfileService profileService,
                         List<FormatChangeValidator> formatChangeValidators) {
        this.formatRepository = formatRepository;
        this.settingsRepository = settingsRepository;
        this.rosterEntryRepository = rosterEntryRepository;
        this.profileService = profileService;
        this.formatChangeValidators = formatChangeValidators;
    }

    // A table has no order of its own, so sort to keep ESPN, Yahoo, Sleeper.
    public List<RosterFormat> getFormats() {
        return formatRepository.findAll().stream()
            .sorted(Comparator.comparingInt(RosterFormat::displayOrder))
            .toList();
    }

    // The user's format, or the default if they've never chosen one.
    public RosterSettings getSettings(String userId) {
        return settingsRepository.getFor(userId);
    }

    // The old slots may not exist in the new format, so a switch moves every player on this
    // user's roster to the bench. Nobody else's roster or format changes.
    // One transaction: either the bench moves and the new format are both saved, or neither is.
    @Transactional
    public RosterSettings updateSettings(String userId, RosterSettings request) {
        String formatId = request.formatId();
        // SRP: a domain exception. ApiExceptionHandler decides it becomes a 400.
        RosterFormat format = Optional.ofNullable(formatId)
            .flatMap(formatRepository::findById)
            .orElseThrow(() -> new UnknownRosterFormatException(formatId));
        formatChangeValidators.forEach(validator -> validator.validate(userId, format));

        for (RosterEntry entry : rosterEntryRepository.findByProfileId(userId)) {
            entry.setSlot(RosterEntry.BENCH);
            rosterEntryRepository.save(entry);
        }

        // The user's first choice adds their row (and their profile, for the foreign key).
        RosterSettings settings = settingsRepository.findByProfileId(userId)
            .orElseGet(() -> new RosterSettings(profileService.getOrCreate(userId), formatId));
        settings.changeFormat(formatId);
        return settingsRepository.save(settings);
    }
}
