package com.sidelined.backend.config;

import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Team;
import com.sidelined.backend.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TeamDataLoaderTest {

    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final TeamDataLoader loader = new TeamDataLoader(teamRepository);

    @SuppressWarnings("unchecked")
    private List<Team> savedTeams() {
        ArgumentCaptor<List<Team>> captor = ArgumentCaptor.forClass(List.class);
        verify(teamRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @Test
    void emptyTable_savesAll32TeamsWithNames() {
        // Arrange
        when(teamRepository.findAll()).thenReturn(List.of());

        // Act
        loader.run(null);

        // Assert
        List<Team> saved = savedTeams();
        assertEquals(32, saved.size());
        assertEquals(32, saved.stream().map(Team::getCode).distinct().count());
        Team arizona = saved.stream().filter(t -> t.getCode() == NflTeam.ARI).findFirst().orElseThrow();
        assertEquals("Arizona Cardinals", arizona.getName());
    }

    @Test
    void fullTable_savesNothing() {
        // Arrange — a restart: every team is already stored.
        List<Team> all = Arrays.stream(NflTeam.values())
            .map(t -> new Team(t, t.getDisplayName()))
            .toList();
        when(teamRepository.findAll()).thenReturn(all);

        // Act
        loader.run(null);

        // Assert
        verify(teamRepository, never()).saveAll(any());
    }

    @Test
    void partialTable_savesOnlyTheMissingTeams() {
        // Arrange
        when(teamRepository.findAll()).thenReturn(List.of(new Team(NflTeam.ARI, "Arizona Cardinals")));

        // Act
        loader.run(null);

        // Assert
        List<Team> saved = savedTeams();
        assertEquals(31, saved.size());
        assertTrue(saved.stream().noneMatch(t -> t.getCode() == NflTeam.ARI));
    }
}
