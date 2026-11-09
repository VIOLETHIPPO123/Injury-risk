package com.sidelined.backend.service;

import com.sidelined.backend.exception.NotAdminException;
import com.sidelined.backend.model.Profile;
import com.sidelined.backend.model.Role;
import com.sidelined.backend.repository.ProfileRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;

    public ProfileService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    // The user's profile, created as USER on their first request.
    public Profile getOrCreate(String userId) {
        return profileRepository.findById(userId).orElseGet(() -> create(userId));
    }

    // The shared admin check: throws (403 via ApiExceptionHandler) unless the user is an ADMIN.
    // A user with no profile yet is not an admin, and no profile is created here.
    public void requireAdmin(String userId) {
        boolean admin = profileRepository.findById(userId)
            .map(profile -> profile.getRole() == Role.ADMIN)
            .orElse(false);
        if (!admin) {
            throw new NotAdminException();
        }
    }

    // Two first requests at once can both try to insert; the loser reads the winner's row.
    private Profile create(String userId) {
        try {
            return profileRepository.save(new Profile(userId));
        } catch (DataIntegrityViolationException e) {
            return profileRepository.findById(userId).orElseThrow(() -> e);
        }
    }
}
