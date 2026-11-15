package com.sidelined.backend.service;

import com.sidelined.backend.exception.RosterTooBigForFormatException;
import com.sidelined.backend.exception.UnknownRosterFormatException;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.model.RosterFormat;
import com.sidelined.backend.model.RosterSettings;
import com.sidelined.backend.repository.RosterEntryRepository;
import com.sidelined.backend.repository.RosterFormatRepository;
import com.sidelined.backend.repository.RosterSettingsRepository;
import com.sidelined.backend.validation.FormatChangeValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;

import static com.sidelined.backend.model.StandardRosterFormats.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RosterServiceTest {

    @Mock
    private RosterFormatRepository formatRepository;

    @Mock
    private RosterSettingsRepository settingsRepository;

    @Mock
    private RosterEntryRepository rosterEntryRepository;

    @Mock
    private FormatChangeValidator formatChangeValidator;

    private RosterService rosterService;

    @BeforeEach
    void setUp() {
        rosterService = new RosterService(formatRepository, settingsRepository,
            rosterEntryRepository, List.of(formatChangeValidator));
    }

    private void formatExists(String id) {
        when(formatRepository.findById(id)).thenReturn(byId(id));
    }

    // ---------- getFormats ----------

    @Test
    void getFormats_returnsThemInDisplayOrder() {
        // Arrange — the table hands them back in no particular order.
        List<RosterFormat> standard = all();
        when(formatRepository.findAll()).thenReturn(List.of(standard.get(2), standard.get(0), standard.get(1)));

        // Act
        List<String> ids = rosterService.getFormats().stream().map(RosterFormat::id).toList();

        // Assert
        assertEquals(List.of(ESPN_STANDARD, YAHOO_STANDARD, SLEEPER_DEFAULT), ids);
    }

    // ---------- getSettings ----------

    @Test
    void getSettings_returnsTheStoredSettings() {
        // Arrange
        when(settingsRepository.get()).thenReturn(new RosterSettings(YAHOO_STANDARD));

        // Act + Assert
        assertEquals(YAHOO_STANDARD, rosterService.getSettings().formatId());
    }

    // ---------- updateSettings ----------

    @Test
    void updateSettings_knownFormat_isSaved() {
        // Arrange
        formatExists(SLEEPER_DEFAULT);
        when(rosterEntryRepository.findAll()).thenReturn(List.of());
        when(settingsRepository.save(any(RosterSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<RosterSettings> captor = ArgumentCaptor.forClass(RosterSettings.class);

        // Act
        RosterSettings result = rosterService.updateSettings(new RosterSettings(SLEEPER_DEFAULT));

        // Assert
        verify(settingsRepository).save(captor.capture());
        assertEquals(SLEEPER_DEFAULT, captor.getValue().formatId());
        assertEquals(SLEEPER_DEFAULT, result.formatId());
    }

    @Test
    void updateSettings_runsTheFormatChangeRulesAgainstTheNewFormat() {
        // Arrange
        formatExists(YAHOO_STANDARD);
        when(rosterEntryRepository.findAll()).thenReturn(List.of());
        ArgumentCaptor<RosterFormat> captor = ArgumentCaptor.forClass(RosterFormat.class);

        // Act
        rosterService.updateSettings(new RosterSettings(YAHOO_STANDARD));

        // Assert
        verify(formatChangeValidator).validate(captor.capture());
        assertEquals(YAHOO_STANDARD, captor.getValue().id());
    }

    @Test
    void updateSettings_unknownFormat_throwsAndSavesNothing() {
        // Arrange
        when(formatRepository.findById("nope")).thenReturn(Optional.empty());

        // Act
        assertThrows(UnknownRosterFormatException.class,
            () -> rosterService.updateSettings(new RosterSettings("nope")));

        // Assert
        verify(settingsRepository, never()).save(any());
        verify(rosterEntryRepository, never()).save(any());
    }

    @Test
    void updateSettings_missingFormat_throwsWithoutAskingTheRepository() {
        // Act
        assertThrows(UnknownRosterFormatException.class,
            () -> rosterService.updateSettings(new RosterSettings(null)));

        // Assert — JPA refuses to look up a null id, so it must never be asked.
        verifyNoInteractions(formatRepository);
    }

    @Test
    void updateSettings_movesEveryPlayerToTheBench() {
        // Arrange
        formatExists(YAHOO_STANDARD);
        List<RosterEntry> roster = List.of(
            new RosterEntry(1L, 1L, "QB", ""),
            new RosterEntry(2L, 2L, "RB", ""),
            new RosterEntry(3L, 28L, RosterEntry.IR, ""));
        when(rosterEntryRepository.findAll()).thenReturn(roster);

        // Act
        rosterService.updateSettings(new RosterSettings(YAHOO_STANDARD));

        // Assert
        assertTrue(roster.stream().allMatch(e -> RosterEntry.BENCH.equals(e.getSlot())));
        roster.forEach(entry -> verify(rosterEntryRepository).save(entry));
    }

    @Test
    void updateSettings_ruleRejects_throwsAndChangesNothing() {
        // Arrange
        formatExists(YAHOO_STANDARD);
        doThrow(new RosterTooBigForFormatException("Yahoo Standard", 15, 16))
            .when(formatChangeValidator).validate(any());

        // Act
        assertThrows(RosterTooBigForFormatException.class,
            () -> rosterService.updateSettings(new RosterSettings(YAHOO_STANDARD)));

        // Assert
        verify(rosterEntryRepository, never()).save(any());
        verify(settingsRepository, never()).save(any());
    }
}
