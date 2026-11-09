package com.sidelined.backend.repository;

import com.sidelined.backend.model.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

// The profiles table, keyed by Supabase user id.
public interface ProfileRepository extends JpaRepository<Profile, String> {
}
