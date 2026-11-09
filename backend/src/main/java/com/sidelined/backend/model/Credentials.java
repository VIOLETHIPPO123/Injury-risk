package com.sidelined.backend.model;

// The body of POST /auth/signup and /auth/signin. Only these two fields are forwarded to Supabase.
public record Credentials(String email, String password) {

    // Keeps the password out of logs and error messages.
    @Override
    public String toString() {
        return "Credentials[email=" + email + ", password=****]";
    }
}
