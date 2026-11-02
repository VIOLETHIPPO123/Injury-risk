package com.sidelined.backend.service;

import com.sidelined.backend.exception.PlayerNotFoundException;
import com.sidelined.backend.exception.RosterEntryNotFoundException;
import com.sidelined.backend.exception.SlotFullException;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.repository.RosterEntryRepository;
import com.sidelined.backend.validation.RosterEntryValidator;
import com.sidelined.backend.validation.SlotMoveValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RosterEntryServiceTest {

    @Mock
    private RosterEntryRepository rosterEntryRepository;

    @Mock
    private RosterEntryValidator validator;

    @Mock
    private SlotMoveValidator moveValidator;

    private RosterEntryService rosterEntryService;

    @BeforeEach
    void setUp() {
        rosterEntryService = new RosterEntryService(rosterEntryRepository, List.of(validator), List.of(moveValidator));
    }

    // Mimics the real repository: assigns id 42 to any entry saved without one.
    private void stubSaveAssigningId() {
        when(rosterEntryRepository.save(any(RosterEntry.class))).thenAnswer(invocation -> {
            RosterEntry saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(42L);
            }
            return saved;
        });
    }

    // ---------- getAllEntries ----------

    @Test
    void getAllEntries_returnsEntriesOrderedById() {
        // Arrange
        RosterEntry later = new RosterEntry(7L, 1L, null, "Added second");
        RosterEntry earlier = new RosterEntry(3L, 1L, null, "Added first");
        when(rosterEntryRepository.findAll()).thenReturn(List.of(later, earlier));

        // Act
        List<RosterEntry> result = rosterEntryService.getAllEntries();

        // Assert
        assertEquals(List.of(earlier, later), result);
    }

    @Test
    void getAllEntries_emptyRoster_returnsEmptyList() {
        // Arrange
        when(rosterEntryRepository.findAll()).thenReturn(List.of());

        // Act
        List<RosterEntry> result = rosterEntryService.getAllEntries();

        // Assert
        assertTrue(result.isEmpty());
    }

    // ---------- createEntry ----------

    @Test
    void createEntry_validPlayer_savesAndReturnsEntryWithId() {
        // Arrange
        stubSaveAssigningId();
        RosterEntry request = new RosterEntry(null, 1L, null, "Monitor snaps");

        // Act
        RosterEntry result = rosterEntryService.createEntry(request);

        // Assert
        assertEquals(42L, result.getId());
        assertEquals(1L, result.getPlayerId());
        assertEquals("Monitor snaps", result.getNote());
        verify(rosterEntryRepository, times(1)).save(any(RosterEntry.class));
    }

    @Test
    void createEntry_newPlayer_goesOnTheBench() {
        // Arrange
        stubSaveAssigningId();
        RosterEntry request = new RosterEntry(null, 1L, "QB", "Trying to start right away");

        // Act
        RosterEntry result = rosterEntryService.createEntry(request);

        // Assert
        assertEquals(RosterEntry.BENCH, result.getSlot());
    }

    @Test
    void createEntry_clientSuppliedId_isIgnored() {
        // Arrange
        stubSaveAssigningId();
        RosterEntry request = new RosterEntry(999L, 1L, null, "Trying to pick my own id");
        ArgumentCaptor<RosterEntry> captor = ArgumentCaptor.forClass(RosterEntry.class);

        // Act
        RosterEntry result = rosterEntryService.createEntry(request);

        // Assert
        verify(rosterEntryRepository).save(captor.capture());
        assertEquals(42L, result.getId());
        assertNotEquals(999L, captor.getValue().getId());
    }

    @Test
    void createEntry_validatorRejects_doesNotSave() {
        // Arrange
        RosterEntry request = new RosterEntry(null, 9999L, null, "No such player");
        doThrow(new PlayerNotFoundException(9999L)).when(validator).validate(request);

        // Act + Assert
        assertThrows(PlayerNotFoundException.class, () -> rosterEntryService.createEntry(request));
        verify(rosterEntryRepository, never()).save(any());
    }

    // ---------- updateEntry ----------

    @Test
    void updateEntry_existingEntry_updatesNote() {
        // Arrange
        RosterEntry existing = new RosterEntry(5L, 1L, null, "Monitor snaps");
        when(rosterEntryRepository.findById(5L)).thenReturn(Optional.of(existing));
        stubSaveAssigningId();
        RosterEntry changes = new RosterEntry(null, null, null, "Snap share dropping");

        // Act
        RosterEntry result = rosterEntryService.updateEntry(5L, changes);

        // Assert
        assertEquals(5L, result.getId());
        assertEquals(1L, result.getPlayerId());
        assertEquals("Snap share dropping", result.getNote());
        verify(rosterEntryRepository).save(existing);
    }

    @Test
    void updateEntry_nullNote_keepsExistingNote() {
        // Arrange
        RosterEntry existing = new RosterEntry(5L, 1L, null, "Monitor snaps");
        when(rosterEntryRepository.findById(5L)).thenReturn(Optional.of(existing));
        stubSaveAssigningId();
        RosterEntry changes = new RosterEntry(null, null, null, null);

        // Act
        RosterEntry result = rosterEntryService.updateEntry(5L, changes);

        // Assert
        assertEquals("Monitor snaps", result.getNote());
        assertEquals(1L, result.getPlayerId());
    }

    @Test
    void updateEntry_missingEntry_throwsNotFound() {
        // Arrange
        when(rosterEntryRepository.findById(999L)).thenReturn(Optional.empty());
        RosterEntry changes = new RosterEntry(null, null, null, "Anything");

        // Act
        assertThrows(RosterEntryNotFoundException.class,
            () -> rosterEntryService.updateEntry(999L, changes));

        // Assert
        verify(rosterEntryRepository, never()).save(any());
    }

    @Test
    void updateEntry_slotInTheBody_isIgnored() {
        // Arrange
        RosterEntry existing = new RosterEntry(5L, 1L, RosterEntry.BENCH, "Monitor snaps");
        when(rosterEntryRepository.findById(5L)).thenReturn(Optional.of(existing));
        stubSaveAssigningId();

        // Act
        RosterEntry result = rosterEntryService.updateEntry(5L, new RosterEntry(null, null, "QB", null));

        // Assert
        assertEquals(RosterEntry.BENCH, result.getSlot());
    }

    // ---------- moveEntry ----------

    @Test
    void moveEntry_allowedMove_savesTheNewSlot() {
        // Arrange
        RosterEntry existing = new RosterEntry(5L, 1L, RosterEntry.BENCH, "");
        when(rosterEntryRepository.findById(5L)).thenReturn(Optional.of(existing));
        stubSaveAssigningId();

        // Act
        RosterEntry result = rosterEntryService.moveEntry(5L, "QB");

        // Assert
        assertEquals("QB", result.getSlot());
        verify(moveValidator).validate(existing, "QB");
        verify(rosterEntryRepository).save(existing);
    }

    @Test
    void moveEntry_validatorRejects_keepsTheOldSlot() {
        // Arrange
        RosterEntry existing = new RosterEntry(5L, 1L, RosterEntry.BENCH, "");
        when(rosterEntryRepository.findById(5L)).thenReturn(Optional.of(existing));
        doThrow(new SlotFullException("QB")).when(moveValidator).validate(existing, "QB");

        // Act
        assertThrows(SlotFullException.class, () -> rosterEntryService.moveEntry(5L, "QB"));

        // Assert
        assertEquals(RosterEntry.BENCH, existing.getSlot());
        verify(rosterEntryRepository, never()).save(any());
    }

    @Test
    void moveEntry_missingEntry_throwsNotFound() {
        // Arrange
        when(rosterEntryRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(RosterEntryNotFoundException.class, () -> rosterEntryService.moveEntry(999L, "QB"));
        verifyNoInteractions(moveValidator);
    }

    // ---------- deleteEntry ----------

    @Test
    void deleteEntry_existingEntry_removesItFromTheRoster() {
        // Arrange
        when(rosterEntryRepository.existsById(5L)).thenReturn(true);

        // Act
        rosterEntryService.deleteEntry(5L);

        // Assert
        verify(rosterEntryRepository, times(1)).deleteById(5L);
    }

    @Test
    void deleteEntry_missingEntry_throwsNotFound() {
        // Arrange
        when(rosterEntryRepository.existsById(999L)).thenReturn(false);

        // Act
        assertThrows(RosterEntryNotFoundException.class,
            () -> rosterEntryService.deleteEntry(999L));

        // Assert
        verify(rosterEntryRepository, never()).deleteById(any());
    }
}
