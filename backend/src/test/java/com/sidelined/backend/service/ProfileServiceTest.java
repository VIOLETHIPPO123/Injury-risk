package com.sidelined.backend.service;

import com.sidelined.backend.exception.NotAdminException;
import com.sidelined.backend.model.Profile;
import com.sidelined.backend.model.Role;
import com.sidelined.backend.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProfileServiceTest {

    private final ProfileRepository profileRepository = mock(ProfileRepository.class);
    private final ProfileService profileService = new ProfileService(profileRepository);

    @Test
    void getOrCreate_newUser_savesAUserProfile() {
        // Arrange
        when(profileRepository.findById("u1")).thenReturn(Optional.empty());
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Profile profile = profileService.getOrCreate("u1");

        // Assert
        assertEquals("u1", profile.getId());
        assertEquals(Role.USER, profile.getRole());
        verify(profileRepository).save(any(Profile.class));
    }

    @Test
    void getOrCreate_existingUser_returnsTheirProfileWithoutSaving() {
        // Arrange
        when(profileRepository.findById("u1")).thenReturn(Optional.of(new Profile("u1", Role.ADMIN)));

        // Act
        Profile profile = profileService.getOrCreate("u1");

        // Assert
        assertEquals(Role.ADMIN, profile.getRole());
        verify(profileRepository, never()).save(any());
    }

    @Test
    void getOrCreate_anotherRequestCreatedItFirst_returnsThatProfile() {
        // Arrange — not there when first read, the insert loses the race, the re-read finds it.
        Profile winner = new Profile("u1");
        when(profileRepository.findById("u1")).thenReturn(Optional.empty(), Optional.of(winner));
        when(profileRepository.save(any(Profile.class))).thenThrow(new DataIntegrityViolationException("duplicate key"));

        // Act + Assert
        assertSame(winner, profileService.getOrCreate("u1"));
    }

    @Test
    void requireAdmin_admin_passes() {
        when(profileRepository.findById("u1")).thenReturn(Optional.of(new Profile("u1", Role.ADMIN)));

        assertDoesNotThrow(() -> profileService.requireAdmin("u1"));
    }

    @Test
    void requireAdmin_user_throws() {
        when(profileRepository.findById("u1")).thenReturn(Optional.of(new Profile("u1", Role.USER)));

        assertThrows(NotAdminException.class, () -> profileService.requireAdmin("u1"));
    }

    @Test
    void requireAdmin_noProfile_throwsWithoutCreatingOne() {
        when(profileRepository.findById("u1")).thenReturn(Optional.empty());

        assertThrows(NotAdminException.class, () -> profileService.requireAdmin("u1"));
        verify(profileRepository, never()).save(any());
    }
}
