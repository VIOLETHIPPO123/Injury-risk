package com.sidelined.backend.service;

import com.sidelined.backend.exception.AuthServiceUnavailableException;
import com.sidelined.backend.model.Credentials;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

// Supabase is replaced by a mock server, so these check exactly what we send and pass back.
class SupabaseAuthClientTest {

    private static final String SUPABASE = "https://example.supabase.co";
    private static final Credentials CREDENTIALS = new Credentials("fan@example.com", "secret123");
    private static final String CREDENTIALS_JSON = "{\"email\":\"fan@example.com\",\"password\":\"secret123\"}";

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer supabase = MockRestServiceServer.bindTo(builder).build();
    private final SupabaseAuthClient client = new SupabaseAuthClient(builder, SUPABASE, "anon-key");

    @Test
    void signIn_postsCredentialsWithTheAnonKey_andReturnsSupabasesResponse() {
        // Arrange
        String tokenResponse = "{\"access_token\":\"jwt-here\",\"token_type\":\"bearer\"}";
        supabase.expect(requestTo(SUPABASE + "/auth/v1/token?grant_type=password"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("apikey", "anon-key"))
            .andExpect(content().json(CREDENTIALS_JSON, true))
            .andRespond(withSuccess(tokenResponse, MediaType.APPLICATION_JSON));

        // Act
        ResponseEntity<String> response = client.signIn(CREDENTIALS);

        // Assert
        supabase.verify();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(tokenResponse, response.getBody());
    }

    @Test
    void signUp_postsToSignup() {
        // Arrange
        supabase.expect(requestTo(SUPABASE + "/auth/v1/signup"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("apikey", "anon-key"))
            .andExpect(content().json(CREDENTIALS_JSON, true))
            .andRespond(withSuccess("{\"id\":\"new-user\"}", MediaType.APPLICATION_JSON));

        // Act
        ResponseEntity<String> response = client.signUp(CREDENTIALS);

        // Assert
        supabase.verify();
        assertEquals("{\"id\":\"new-user\"}", response.getBody());
    }

    @Test
    void signIn_wrongPassword_passesSupabasesErrorThrough() {
        // Arrange
        String error = "{\"error\":\"invalid_grant\",\"error_description\":\"Invalid login credentials\"}";
        supabase.expect(requestTo(SUPABASE + "/auth/v1/token?grant_type=password"))
            .andRespond(withBadRequest().body(error).contentType(MediaType.APPLICATION_JSON));

        // Act
        ResponseEntity<String> response = client.signIn(CREDENTIALS);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(error, response.getBody());
    }

    @Test
    void signIn_supabaseUnreachable_throwsUnavailable() {
        // Arrange
        supabase.expect(requestTo(SUPABASE + "/auth/v1/token?grant_type=password"))
            .andRespond(withException(new IOException("connection refused")));

        // Act + Assert
        assertThrows(AuthServiceUnavailableException.class, () -> client.signIn(CREDENTIALS));
    }

    @Test
    void credentials_toString_hidesThePassword() {
        assertFalse(CREDENTIALS.toString().contains("secret123"));
    }
}
