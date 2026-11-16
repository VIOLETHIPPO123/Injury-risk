package com.sidelined.backend.config;

import com.sidelined.backend.model.RosterFormat;
import com.sidelined.backend.model.StandardRosterFormats;
import com.sidelined.backend.repository.JpaRosterFormatRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.List;

import static com.sidelined.backend.model.StandardRosterFormats.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// The loader only adds formats. Users' settings rows are made when each user first picks a
// format, which RosterServiceTest and RosterPersistenceTest cover.
class RosterDataLoaderTest {

    private final JpaRosterFormatRepository formatRepository = mock(JpaRosterFormatRepository.class);
    private final RosterDataLoader loader = new RosterDataLoader(formatRepository);

    @SuppressWarnings("unchecked")
    private List<String> savedFormatIds() {
        ArgumentCaptor<List<RosterFormat>> captor = ArgumentCaptor.forClass(List.class);
        verify(formatRepository).saveAll(captor.capture());
        return captor.getValue().stream().map(RosterFormat::id).toList();
    }

    @Test
    void emptyTable_savesTheThreeFormats() {
        // Arrange
        when(formatRepository.findAll()).thenReturn(List.of());

        // Act
        loader.run(null);

        // Assert
        assertEquals(List.of(ESPN_STANDARD, YAHOO_STANDARD, SLEEPER_DEFAULT), savedFormatIds());
    }

    @Test
    void fullTable_savesNothing() {
        // Arrange — a restart: every format is already stored.
        when(formatRepository.findAll()).thenReturn(StandardRosterFormats.all());

        // Act
        loader.run(null);

        // Assert
        verify(formatRepository, never()).saveAll(any());
    }

    @Test
    void partialTable_savesOnlyTheMissingFormats() {
        // Arrange
        when(formatRepository.findAll()).thenReturn(List.of(byId(ESPN_STANDARD).orElseThrow()));

        // Act
        loader.run(null);

        // Assert
        assertEquals(List.of(YAHOO_STANDARD, SLEEPER_DEFAULT), savedFormatIds());
    }
}
