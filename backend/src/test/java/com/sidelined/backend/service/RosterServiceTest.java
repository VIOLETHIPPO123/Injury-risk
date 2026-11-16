package com.sidelined.backend.service;

import com.sidelined.backend.exception.RosterTooBigForFormatException;
import com.sidelined.backend.exception.UnknownRosterFormatException;
import com.sidelined.backend.model.Profile;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RosterServiceTest {

    // The signed-in user.
    private static final String USER = "user-a";

    @Mock
    private RosterFormatRepository formatRepository;

    @Mock
    private RosterSettingsRepository settingsRepository;

    @Mock
    private RosterEntryRepository rosterEntryRepository;

    @Mock
    private ProfileService profileService;

    @Mock
    private FormatChangeValidator formatChangeValidator;

    private RosterService rosterService;

    @BeforeEach
    void setUp() {
        rosterService = new RosterService(formatRepository, settingsRepository,
            rosterEntryRepository, profileService, List.of(formatChangeValidator));
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
        when(settingsRepository.getFor(USER)).thenReturn(new RosterSettings(YAHOO_STANDARD));

        // Act + Assert
        assertEquals(YAHOO_STANDARD, rosterService.getSettings(USER).formatId());
    }

    // ---------- updateSettings ----------

    @Test
    void updateSettings_knownFormat_isSaved() {
        // Arrange
        formatExists(SLEEPER_DEFAULT);
        when(rosterEntryRepository.findByProfileId(USER)).thenReturn(List.of());
        when(settingsRepository.save(any(RosterSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<RosterSettings> captor = ArgumentCaptor.forClass(RosterSettings.class);

        // Act
        RosterSettings result = rosterService.updateSettings(USER, new RosterSettings(SLEEPER_DEFAULT));

        // Assert
        verify(settingsRepository).save(captor.capture());
        assertEquals(SLEEPER_DEFAULT, captor.getValue().formatId());
        assertEquals(SLEEPER_DEFAULT, result.formatId());
    }

    @Test
    void updateSettings_runsTheFormatChangeRulesAgainstTheNewFormat() {
        // Arrange
        formatExists(YAHOO_STANDARD);
        when(rosterEntryRepository.findByProfileId(USER)).thenReturn(List.of());
        ArgumentCaptor<RosterFormat> captor = ArgumentCaptor.forClass(RosterFormat.class);

        // Act
        rosterService.updateSettings(USER, new RosterSettings(YAHOO_STANDARD));

        // Assert
        verify(formatChangeValidator).validate(eq(USER), captor.capture());
        assertEquals(YAHOO_STANDARD, captor.getValue().id());
    }

    @Test
    void updateSettings_unknownFormat_throwsAndSavesNothing() {
        // Arrange
        when(formatRepository.findById("nope")).thenReturn(Optional.empty());

        // Act
        assertThrows(UnknownRosterFormatException.class,
            () -> rosterService.updateSettings(USER, new RosterSettings("nope")));

        // Assert
        verify(settingsRepository, never()).save(any());
        verify(rosterEntryRepository, never()).save(any());
    }

    @Test
    void updateSettings_missingFormat_throwsWithoutAskingTheRepository() {
        // Act
        assertThrows(UnknownRosterFormatException.class,
            () -> rosterService.updateSettings(USER, new RosterSettings(null)));

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
        when(rosterEntryRepository.findByProfileId(USER)).thenReturn(roster);

        // Act
        rosterService.updateSettings(USER, new RosterSettings(YAHOO_STANDARD));

        // Assert
        assertTrue(roster.stream().allMatch(e -> RosterEntry.BENCH.equals(e.getSlot())));
        roster.forEach(entry -> verify(rosterEntryRepository).save(entry));
    }

    @Test
    void updateSettings_ruleRejects_throwsAndChangesNothing() {
        // Arrange
        formatExists(YAHOO_STANDARD);
        doThrow(new RosterTooBigForFormatException("Yahoo Standard", 15, 16))
            .when(formatChangeValidator).validate(any(), any());

        // Act
        assertThrows(RosterTooBigForFormatException.class,
            () -> rosterService.updateSettings(USER, new RosterSettings(YAHOO_STANDARD)));

        // Assert
        verify(rosterEntryRepository, never()).save(any());
        verify(settingsRepository, never()).save(any());
    }

    // ---------- One row per user ----------

    @Test
    void updateSettings_firstChoice_savesANewRowLinkedToTheUsersProfile() {
        // Arrange — USER has never chosen a format.
        Profile profile = new Profile(USER);
        formatExists(YAHOO_STANDARD);
        when(settingsRepository.findByProfileId(USER)).thenReturn(Optional.empty());
        when(profileService.getOrCreate(USER)).thenReturn(profile);
        when(settingsRepository.save(any(RosterSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        rosterService.updateSettings(USER, new RosterSettings(YAHOO_STANDARD));

        // Assert
        ArgumentCaptor<RosterSettings> captor = ArgumentCaptor.forClass(RosterSettings.class);
        verify(settingsRepository).save(captor.capture());
        assertSame(profile, captor.getValue().profile());
        assertEquals(YAHOO_STANDARD, captor.getValue().formatId());
    }

    @Test
    void updateSettings_laterChoice_updatesTheUsersExistingRow() {
        // Arrange — USER already has a row.
        RosterSettings existing = new RosterSettings(new Profile(USER), ESPN_STANDARD);
        formatExists(SLEEPER_DEFAULT);
        when(settingsRepository.findByProfileId(USER)).thenReturn(Optional.of(existing));

        // Act
        rosterService.updateSettings(USER, new RosterSettings(SLEEPER_DEFAULT));

        // Assert — the same row, not a second one.
        verify(settingsRepository).save(existing);
        assertEquals(SLEEPER_DEFAULT, existing.formatId());
        verifyNoInteractions(profileService);
    }

    @Test
    void updateSettings_benchesOnlyTheUsersOwnPlayers() {
        // Arrange
        formatExists(YAHOO_STANDARD);

        // Act
        rosterService.updateSettings(USER, new RosterSettings(YAHOO_STANDARD));

        // Assert — other users' entries are never even read.
        verify(rosterEntryRepository).findByProfileId(USER);
        verify(rosterEntryRepository, never()).findAll();
    }
}
