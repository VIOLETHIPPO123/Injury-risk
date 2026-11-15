package com.sidelined.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import static org.junit.jupiter.api.Assertions.*;

class GoogleSignInTest {

    private final GoogleSignIn googleSignIn =
        new GoogleSignIn("https://project.supabase.co", "http://localhost:5173");

    @Test
    void authorizeUri_isSupabasesAuthorizeEndpointForGoogle_returningToTheFrontend() {
        UriComponents uri = UriComponentsBuilder.fromUri(googleSignIn.authorizeUri()).build();

        assertEquals("https", uri.getScheme());
        assertEquals("project.supabase.co", uri.getHost());
        assertEquals("/auth/v1/authorize", uri.getPath());
        assertEquals("google", uri.getQueryParams().getFirst("provider"));
        assertEquals("http://localhost:5173", uri.getQueryParams().getFirst("redirect_to"));
    }

    @Test
    void authorizeUri_carriesNoSecret() {
        String uri = googleSignIn.authorizeUri().toString();

        // Only the provider and where to come back to: the client id and secret are Supabase's.
        assertFalse(uri.contains("client_secret"));
        assertFalse(uri.contains("apikey"));
    }
}
