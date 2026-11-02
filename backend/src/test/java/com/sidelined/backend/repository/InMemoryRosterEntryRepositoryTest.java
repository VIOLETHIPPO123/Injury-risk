package com.sidelined.backend.repository;

import com.sidelined.backend.model.RosterEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryRosterEntryRepositoryTest {

    private final InMemoryRosterEntryRepository rosterEntryRepository = new InMemoryRosterEntryRepository();

    @Test
    void save_assignsIdAndMakesEntryFindable() {
        // Act
        RosterEntry saved = rosterEntryRepository.save(new RosterEntry(null, 1L, RosterEntry.BENCH, "Monitor snaps"));

        // Assert
        assertNotNull(saved.getId());
        assertTrue(rosterEntryRepository.existsById(saved.getId()));
        assertEquals(saved, rosterEntryRepository.findById(saved.getId()).orElseThrow());
    }

    @Test
    void deleteById_removesTheEntry() {
        // Arrange
        RosterEntry saved = rosterEntryRepository.save(new RosterEntry(null, 1L, RosterEntry.BENCH, ""));

        // Act
        rosterEntryRepository.deleteById(saved.getId());

        // Assert
        assertFalse(rosterEntryRepository.existsById(saved.getId()));
    }

    @Test
    void deleteById_missingId_isANoOp() {
        // Act + Assert — matches Spring Data; the service checks existsById to answer 404.
        assertDoesNotThrow(() -> rosterEntryRepository.deleteById(999L));
        assertTrue(rosterEntryRepository.findAll().isEmpty());
    }
}
