package com.sidelined.backend.config;

import com.sidelined.backend.model.RosterFormat;
import com.sidelined.backend.model.RosterSettings;
import com.sidelined.backend.model.StandardRosterFormats;
import com.sidelined.backend.repository.JpaRosterFormatRepository;
import com.sidelined.backend.repository.JpaRosterSettingsRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.List;

import static com.sidelined.backend.model.StandardRosterFormats.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RosterDataLoaderTest {

    private final JpaRosterFormatRepository formatRepository = mock(JpaRosterFormatRepository.class);
    private final JpaRosterSettingsRepository settingsRepository = mock(JpaRosterSettingsRepository.class);
    private final RosterDataLoader loader = new RosterDataLoader(formatRepository, settingsRepository);

    @SuppressWarnings("unchecked")
    private List<String> savedFormatIds() {
        ArgumentCaptor<List<RosterFormat>> captor = ArgumentCaptor.forClass(List.class);
        verify(formatRepository).saveAll(captor.capture());
        return captor.getValue().stream().map(RosterFormat::id).toList();
    }

    @Test
    void emptyTables_saveTheThreeFormatsAndTheDefaultSettings() {
        // Arrange
        when(formatRepository.findAll()).thenReturn(List.of());
        when(settingsRepository.existsById(RosterSettings.ID)).thenReturn(false);
        ArgumentCaptor<RosterSettings> settings = ArgumentCaptor.forClass(RosterSettings.class);

        // Act
        loader.run(null);

        // Assert
        assertEquals(List.of(ESPN_STANDARD, YAHOO_STANDARD, SLEEPER_DEFAULT), savedFormatIds());
        verify(settingsRepository).save(settings.capture());
        assertEquals(ESPN_STANDARD, settings.getValue().formatId());
    }

    @Test
    void fullTables_saveNothing() {
        // Arrange — a restart: everything is already stored.
        when(formatRepository.findAll()).thenReturn(StandardRosterFormats.all());
        when(settingsRepository.existsById(RosterSettings.ID)).thenReturn(true);

        // Act
        loader.run(null);

        // Assert
        verify(formatRepository, never()).saveAll(any());
        verify(settingsRepository, never()).save(any(RosterSettings.class));
    }

    @Test
    void partialTable_savesOnlyTheMissingFormats() {
        // Arrange
        when(formatRepository.findAll()).thenReturn(List.of(byId(ESPN_STANDARD).orElseThrow()));
        when(settingsRepository.existsById(RosterSettings.ID)).thenReturn(true);

        // Act
        loader.run(null);

        // Assert
        assertEquals(List.of(YAHOO_STANDARD, SLEEPER_DEFAULT), savedFormatIds());
    }

    @Test
    void existingSettings_areNotReset() {
        // Arrange — the league has already switched formats.
        when(formatRepository.findAll()).thenReturn(StandardRosterFormats.all());
        when(settingsRepository.existsById(RosterSettings.ID)).thenReturn(true);

        // Act
        loader.run(null);

        // Assert
        verify(settingsRepository, never()).save(any(RosterSettings.class));
    }
}
