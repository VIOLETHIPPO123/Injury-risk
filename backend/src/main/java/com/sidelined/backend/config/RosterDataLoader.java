package com.sidelined.backend.config;

import com.sidelined.backend.model.RosterFormat;
import com.sidelined.backend.model.RosterSettings;
import com.sidelined.backend.model.StandardRosterFormats;
import com.sidelined.backend.repository.JpaRosterFormatRepository;
import com.sidelined.backend.repository.JpaRosterSettingsRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// At startup, adds any of the standard roster formats that are missing from roster_formats,
// and the settings row if there isn't one. A restart adds nothing, so there are never duplicates.
// A format already in the table is left as it is: to change one, edit or delete its row.
@Component
public class RosterDataLoader implements ApplicationRunner {

    private final JpaRosterFormatRepository formatRepository;
    private final JpaRosterSettingsRepository settingsRepository;

    public RosterDataLoader(JpaRosterFormatRepository formatRepository,
                            JpaRosterSettingsRepository settingsRepository) {
        this.formatRepository = formatRepository;
        this.settingsRepository = settingsRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        Set<String> stored = formatRepository.findAll().stream()
            .map(RosterFormat::id)
            .collect(Collectors.toSet());

        List<RosterFormat> missing = StandardRosterFormats.all().stream()
            .filter(format -> !stored.contains(format.id()))
            .toList();

        if (!missing.isEmpty()) {
            formatRepository.saveAll(missing);
        }

        if (!settingsRepository.existsById(RosterSettings.ID)) {
            settingsRepository.save(RosterSettings.defaults());
        }
    }
}
