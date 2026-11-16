package com.sidelined.backend.service;

import com.sidelined.backend.exception.PlayerNotFoundException;
import com.sidelined.backend.exception.RosterEntryNotFoundException;
import com.sidelined.backend.exception.SlotFullException;
import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Player;
import com.sidelined.backend.model.Position;
import com.sidelined.backend.model.Profile;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.model.Workload;
import com.sidelined.backend.repository.PlayerRepository;
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

    // The signed-in user, and someone else.
    private static final String USER = "user-a";
    private static final String OTHER_USER = "user-b";

    @Mock
    private RosterEntryRepository rosterEntryRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private ProfileService profileService;

    @Mock
    private RosterEntryValidator validator;

    @Mock
    private SlotMoveValidator moveValidator;

    private RosterEntryService rosterEntryService;

    @BeforeEach
    void setUp() {
        rosterEntryService = new RosterEntryService(
            rosterEntryRepository, playerRepository, profileService, List.of(validator), List.of(moveValidator));
    }

    private static final Player BRISSETT =
        new Player(1L, "Jacoby Brissett", NflTeam.ARI, Position.QB, new Workload(68, 253));

    private void playerExists(Player player) {
        when(playerRepository.findById(player.getId())).thenReturn(Optional.of(player));
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
        when(rosterEntryRepository.findByProfileId(USER)).thenReturn(List.of(later, earlier));

        // Act
        List<RosterEntry> result = rosterEntryService.getAllEntries(USER);

        // Assert
        assertEquals(List.of(earlier, later), result);
    }

    @Test
    void getAllEntries_emptyRoster_returnsEmptyList() {
        // Arrange
        when(rosterEntryRepository.findByProfileId(USER)).thenReturn(List.of());

        // Act
        List<RosterEntry> result = rosterEntryService.getAllEntries(USER);

        // Assert
        assertTrue(result.isEmpty());
    }

    // ---------- createEntry ----------

    @Test
    void createEntry_validPlayer_savesAndReturnsEntryWithId() {
        // Arrange
        playerExists(BRISSETT);
        stubSaveAssigningId();
        RosterEntry request = new RosterEntry(null, 1L, null, "Monitor snaps");

        // Act
        RosterEntry result = rosterEntryService.createEntry(USER, request);

        // Assert
        assertEquals(42L, result.getId());
        assertEquals(1L, result.getPlayerId());
        assertEquals("Monitor snaps", result.getNote());
        verify(rosterEntryRepository, times(1)).save(any(RosterEntry.class));
    }

    @Test
    void createEntry_linksTheSavedEntryToThePlayer() {
        // Arrange
        playerExists(BRISSETT);
        stubSaveAssigningId();
        ArgumentCaptor<RosterEntry> captor = ArgumentCaptor.forClass(RosterEntry.class);

        // Act
        rosterEntryService.createEntry(USER, new RosterEntry(null, 1L, null, ""));

        // Assert — the player link is what the database stores as the foreign key.
        verify(rosterEntryRepository).save(captor.capture());
        assertSame(BRISSETT, captor.getValue().getPlayer());
    }

    @Test
    void createEntry_playerGoneAfterValidation_throwsNotFoundAndDoesNotSave() {
        // Arrange — the validators passed, but the player is no longer in the table.
        when(playerRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        assertThrows(PlayerNotFoundException.class,
            () -> rosterEntryService.createEntry(USER, new RosterEntry(null, 1L, null, "")));

        // Assert
        verify(rosterEntryRepository, never()).save(any());
    }

    @Test
    void createEntry_newPlayer_goesOnTheBench() {
        // Arrange
        playerExists(BRISSETT);
        stubSaveAssigningId();
        RosterEntry request = new RosterEntry(null, 1L, "QB", "Trying to start right away");

        // Act
        RosterEntry result = rosterEntryService.createEntry(USER, request);

        // Assert
        assertEquals(RosterEntry.BENCH, result.getSlot());
    }

    @Test
    void createEntry_clientSuppliedId_isIgnored() {
        // Arrange
        playerExists(BRISSETT);
        stubSaveAssigningId();
        RosterEntry request = new RosterEntry(999L, 1L, null, "Trying to pick my own id");
        ArgumentCaptor<RosterEntry> captor = ArgumentCaptor.forClass(RosterEntry.class);

        // Act
        RosterEntry result = rosterEntryService.createEntry(USER, request);

        // Assert
        verify(rosterEntryRepository).save(captor.capture());
        assertEquals(42L, result.getId());
        assertNotEquals(999L, captor.getValue().getId());
    }

    @Test
    void createEntry_validatorRejects_doesNotSave() {
        // Arrange
        RosterEntry request = new RosterEntry(null, 9999L, null, "No such player");
        doThrow(new PlayerNotFoundException(9999L)).when(validator).validate(USER, request);

        // Act + Assert
        assertThrows(PlayerNotFoundException.class, () -> rosterEntryService.createEntry(USER, request));
        verify(rosterEntryRepository, never()).save(any());
        verifyNoInteractions(playerRepository);
    }

    // ---------- updateEntry ----------

    @Test
    void updateEntry_existingEntry_updatesNote() {
        // Arrange
        RosterEntry existing = new RosterEntry(5L, 1L, null, "Monitor snaps");
        when(rosterEntryRepository.findByIdAndProfileId(5L, USER)).thenReturn(Optional.of(existing));
        stubSaveAssigningId();
        RosterEntry changes = new RosterEntry(null, null, null, "Snap share dropping");

        // Act
        RosterEntry result = rosterEntryService.updateEntry(USER, 5L, changes);

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
        when(rosterEntryRepository.findByIdAndProfileId(5L, USER)).thenReturn(Optional.of(existing));
        stubSaveAssigningId();
        RosterEntry changes = new RosterEntry(null, null, null, null);

        // Act
        RosterEntry result = rosterEntryService.updateEntry(USER, 5L, changes);

        // Assert
        assertEquals("Monitor snaps", result.getNote());
        assertEquals(1L, result.getPlayerId());
    }

    @Test
    void updateEntry_missingEntry_throwsNotFound() {
        // Arrange
        when(rosterEntryRepository.findByIdAndProfileId(999L, USER)).thenReturn(Optional.empty());
        RosterEntry changes = new RosterEntry(null, null, null, "Anything");

        // Act
        assertThrows(RosterEntryNotFoundException.class,
            () -> rosterEntryService.updateEntry(USER, 999L, changes));

        // Assert
        verify(rosterEntryRepository, never()).save(any());
    }

    @Test
    void updateEntry_slotInTheBody_isIgnored() {
        // Arrange
        RosterEntry existing = new RosterEntry(5L, 1L, RosterEntry.BENCH, "Monitor snaps");
        when(rosterEntryRepository.findByIdAndProfileId(5L, USER)).thenReturn(Optional.of(existing));
        stubSaveAssigningId();

        // Act
        RosterEntry result = rosterEntryService.updateEntry(USER, 5L, new RosterEntry(null, null, "QB", null));

        // Assert
        assertEquals(RosterEntry.BENCH, result.getSlot());
    }

    // ---------- moveEntry ----------

    @Test
    void moveEntry_allowedMove_savesTheNewSlot() {
        // Arrange
        RosterEntry existing = new RosterEntry(5L, 1L, RosterEntry.BENCH, "");
        when(rosterEntryRepository.findByIdAndProfileId(5L, USER)).thenReturn(Optional.of(existing));
        stubSaveAssigningId();

        // Act
        RosterEntry result = rosterEntryService.moveEntry(USER, 5L, "QB");

        // Assert
        assertEquals("QB", result.getSlot());
        verify(moveValidator).validate(USER, existing, "QB");
        verify(rosterEntryRepository).save(existing);
    }

    @Test
    void moveEntry_validatorRejects_keepsTheOldSlot() {
        // Arrange
        RosterEntry existing = new RosterEntry(5L, 1L, RosterEntry.BENCH, "");
        when(rosterEntryRepository.findByIdAndProfileId(5L, USER)).thenReturn(Optional.of(existing));
        doThrow(new SlotFullException("QB")).when(moveValidator).validate(USER, existing, "QB");

        // Act
        assertThrows(SlotFullException.class, () -> rosterEntryService.moveEntry(USER, 5L, "QB"));

        // Assert
        assertEquals(RosterEntry.BENCH, existing.getSlot());
        verify(rosterEntryRepository, never()).save(any());
    }

    @Test
    void moveEntry_missingEntry_throwsNotFound() {
        // Arrange
        when(rosterEntryRepository.findByIdAndProfileId(999L, USER)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(RosterEntryNotFoundException.class, () -> rosterEntryService.moveEntry(USER, 999L, "QB"));
        verifyNoInteractions(moveValidator);
    }

    // ---------- deleteEntry ----------

    @Test
    void deleteEntry_existingEntry_removesItFromTheRoster() {
        // Arrange
        when(rosterEntryRepository.findByIdAndProfileId(5L, USER))
            .thenReturn(Optional.of(new RosterEntry(5L, 1L, RosterEntry.BENCH, "")));

        // Act
        rosterEntryService.deleteEntry(USER, 5L);

        // Assert
        verify(rosterEntryRepository, times(1)).deleteById(5L);
    }

    @Test
    void deleteEntry_missingEntry_throwsNotFound() {
        // Arrange
        when(rosterEntryRepository.findByIdAndProfileId(999L, USER)).thenReturn(Optional.empty());

        // Act
        assertThrows(RosterEntryNotFoundException.class,
            () -> rosterEntryService.deleteEntry(USER, 999L));

        // Assert
        verify(rosterEntryRepository, never()).deleteById(any());
    }

    // ---------- Whose roster ----------

    @Test
    void getAllEntries_readsOnlyTheUsersRoster() {
        // Act
        rosterEntryService.getAllEntries(USER);

        // Assert
        verify(rosterEntryRepository).findByProfileId(USER);
        verify(rosterEntryRepository, never()).findAll();
    }

    @Test
    void createEntry_linksTheEntryToTheSignedInUsersProfile() {
        // Arrange
        Profile profile = new Profile(USER);
        playerExists(BRISSETT);
        stubSaveAssigningId();
        when(profileService.getOrCreate(USER)).thenReturn(profile);
        ArgumentCaptor<RosterEntry> captor = ArgumentCaptor.forClass(RosterEntry.class);

        // Act
        rosterEntryService.createEntry(USER, new RosterEntry(null, 1L, null, ""));

        // Assert
        verify(rosterEntryRepository).save(captor.capture());
        assertSame(profile, captor.getValue().getProfile());
    }

    @Test
    void anotherUsersEntry_isNotFound_forNotesMovesOrRemoving() {
        // Arrange — entry 5 is on OTHER_USER's roster, so it isn't on USER's.
        when(rosterEntryRepository.findByIdAndProfileId(5L, USER)).thenReturn(Optional.empty());

        // Act + Assert — the same exception as an id that doesn't exist at all.
        assertThrows(RosterEntryNotFoundException.class,
            () -> rosterEntryService.updateEntry(USER, 5L, new RosterEntry(null, null, null, "x")));
        assertThrows(RosterEntryNotFoundException.class, () -> rosterEntryService.moveEntry(USER, 5L, "QB"));
        assertThrows(RosterEntryNotFoundException.class, () -> rosterEntryService.deleteEntry(USER, 5L));
        verify(rosterEntryRepository, never()).save(any());
        verify(rosterEntryRepository, never()).deleteById(any());
        verifyNoInteractions(moveValidator);
    }

    @Test
    void rules_areCheckedForTheSignedInUser() {
        // Arrange
        RosterEntry request = new RosterEntry(null, 1L, null, "");
        RosterEntry existing = new RosterEntry(5L, 1L, RosterEntry.BENCH, "");
        playerExists(BRISSETT);
        stubSaveAssigningId();
        when(rosterEntryRepository.findByIdAndProfileId(5L, OTHER_USER)).thenReturn(Optional.of(existing));

        // Act
        rosterEntryService.createEntry(OTHER_USER, request);
        rosterEntryService.moveEntry(OTHER_USER, 5L, "QB");

        // Assert
        verify(validator).validate(OTHER_USER, request);
        verify(moveValidator).validate(OTHER_USER, existing, "QB");
    }
}
