package com.sidelined.backend.model;

// What a signed-in user may do. Everyone starts as USER; an ADMIN is promoted by hand in the
// Supabase Table Editor (profiles.role), never through the API.
public enum Role {
    USER,
    ADMIN
}
