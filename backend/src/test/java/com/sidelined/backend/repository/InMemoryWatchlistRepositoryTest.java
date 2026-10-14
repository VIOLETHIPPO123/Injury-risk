package com.sidelined.backend.repository;

import com.sidelined.backend.model.WatchlistEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryWatchlistRepositoryTest {

    private final InMemoryWatchlistRepository watchlistRepository = new InMemoryWatchlistRepository();

    @Test
    void save_assignsIdAndMakesEntryFindable() {
        // Act
        WatchlistEntry saved = watchlistRepository.save(new WatchlistEntry(null, 1L, "Monitor snaps"));

        // Assert
        assertNotNull(saved.getId());
        assertTrue(watchlistRepository.existsById(saved.getId()));
        assertEquals(saved, watchlistRepository.findById(saved.getId()).orElseThrow());
    }

    @Test
    void deleteById_removesTheEntry() {
        // Arrange
        WatchlistEntry saved = watchlistRepository.save(new WatchlistEntry(null, 1L, ""));

        // Act
        watchlistRepository.deleteById(saved.getId());

        // Assert
        assertFalse(watchlistRepository.existsById(saved.getId()));
    }

    @Test
    void deleteById_missingId_isANoOp() {
        // Act + Assert — matches Spring Data; the service checks existsById to answer 404.
        assertDoesNotThrow(() -> watchlistRepository.deleteById(999L));
        assertTrue(watchlistRepository.findAll().isEmpty());
    }
}
