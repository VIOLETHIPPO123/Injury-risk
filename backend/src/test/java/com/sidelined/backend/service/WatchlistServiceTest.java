package com.sidelined.backend.service;

import com.sidelined.backend.model.Player;
import com.sidelined.backend.model.WatchlistEntry;
import com.sidelined.backend.repository.WatchlistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceTest {

    @Mock
    private WatchlistRepository watchlistRepository;

    @Mock
    private PlayerService playerService;

    @InjectMocks
    private WatchlistService watchlistService;

    private static final Player KNOWN_PLAYER = new Player(1L, "Jacoby Brissett", "ARI", "QB", 68, 253);

    // Mimics the real repository: assigns id 42 to any entry saved without one.
    private void stubSaveAssigningId() {
        when(watchlistRepository.save(any(WatchlistEntry.class))).thenAnswer(invocation -> {
            WatchlistEntry saved = invocation.getArgument(0);
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
        WatchlistEntry later = new WatchlistEntry(7L, 1L, "Added second");
        WatchlistEntry earlier = new WatchlistEntry(3L, 1L, "Added first");
        when(watchlistRepository.findAll()).thenReturn(List.of(later, earlier));

        // Act
        List<WatchlistEntry> result = watchlistService.getAllEntries();

        // Assert
        assertEquals(List.of(earlier, later), result);
    }

    @Test
    void getAllEntries_emptyWatchlist_returnsEmptyList() {
        // Arrange
        when(watchlistRepository.findAll()).thenReturn(List.of());

        // Act
        List<WatchlistEntry> result = watchlistService.getAllEntries();

        // Assert
        assertTrue(result.isEmpty());
    }

    // ---------- createEntry ----------

    @Test
    void createEntry_validPlayer_savesAndReturnsEntryWithId() {
        // Arrange
        when(playerService.getAllPlayers()).thenReturn(List.of(KNOWN_PLAYER));
        stubSaveAssigningId();
        WatchlistEntry request = new WatchlistEntry(null, 1L, "Monitor snaps");

        // Act
        WatchlistEntry result = watchlistService.createEntry(request);

        // Assert
        assertEquals(42L, result.getId());
        assertEquals(1L, result.getPlayerId());
        assertEquals("Monitor snaps", result.getNote());
        verify(watchlistRepository, times(1)).save(any(WatchlistEntry.class));
    }

    @Test
    void createEntry_clientSuppliedId_isIgnored() {
        // Arrange
        when(playerService.getAllPlayers()).thenReturn(List.of(KNOWN_PLAYER));
        stubSaveAssigningId();
        WatchlistEntry request = new WatchlistEntry(999L, 1L, "Trying to pick my own id");
        ArgumentCaptor<WatchlistEntry> captor = ArgumentCaptor.forClass(WatchlistEntry.class);

        // Act
        WatchlistEntry result = watchlistService.createEntry(request);

        // Assert
        verify(watchlistRepository).save(captor.capture());
        assertEquals(42L, result.getId());
        assertNotEquals(999L, captor.getValue().getId());
    }

    @Test
    void createEntry_unknownPlayer_throwsNotFound() {
        // Arrange
        when(playerService.getAllPlayers()).thenReturn(List.of(KNOWN_PLAYER));
        WatchlistEntry request = new WatchlistEntry(null, 9999L, "No such player");

        // Act
        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
            () -> watchlistService.createEntry(request));

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatusCode());
        verify(watchlistRepository, never()).save(any());
    }

    // ---------- updateEntry ----------

    @Test
    void updateEntry_existingEntry_updatesNote() {
        // Arrange
        WatchlistEntry existing = new WatchlistEntry(5L, 1L, "Monitor snaps");
        when(watchlistRepository.findById(5L)).thenReturn(Optional.of(existing));
        stubSaveAssigningId();
        WatchlistEntry changes = new WatchlistEntry(null, null, "Snap share dropping");

        // Act
        WatchlistEntry result = watchlistService.updateEntry(5L, changes);

        // Assert
        assertEquals(5L, result.getId());
        assertEquals(1L, result.getPlayerId());
        assertEquals("Snap share dropping", result.getNote());
        verify(watchlistRepository).save(existing);
    }

    @Test
    void updateEntry_nullNote_keepsExistingNote() {
        // Arrange
        WatchlistEntry existing = new WatchlistEntry(5L, 1L, "Monitor snaps");
        when(watchlistRepository.findById(5L)).thenReturn(Optional.of(existing));
        stubSaveAssigningId();
        WatchlistEntry changes = new WatchlistEntry(null, null, null);

        // Act
        WatchlistEntry result = watchlistService.updateEntry(5L, changes);

        // Assert
        assertEquals("Monitor snaps", result.getNote());
        assertEquals(1L, result.getPlayerId());
    }

    @Test
    void updateEntry_missingEntry_throwsNotFound() {
        // Arrange
        when(watchlistRepository.findById(999L)).thenReturn(Optional.empty());
        WatchlistEntry changes = new WatchlistEntry(null, null, "Anything");

        // Act
        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
            () -> watchlistService.updateEntry(999L, changes));

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatusCode());
        verify(watchlistRepository, never()).save(any());
    }

    // ---------- deleteEntry ----------

    @Test
    void deleteEntry_existingEntry_removesItFromTheWatchlist() {
        // Arrange
        when(watchlistRepository.deleteById(5L)).thenReturn(true);

        // Act
        watchlistService.deleteEntry(5L);

        // Assert
        verify(watchlistRepository, times(1)).deleteById(5L);
    }

    @Test
    void deleteEntry_missingEntry_throwsNotFound() {
        // Arrange
        when(watchlistRepository.deleteById(999L)).thenReturn(false);

        // Act
        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
            () -> watchlistService.deleteEntry(999L));

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatusCode());
    }
}
