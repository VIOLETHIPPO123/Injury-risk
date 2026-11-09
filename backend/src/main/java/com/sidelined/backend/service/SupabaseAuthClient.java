package com.sidelined.backend.service;

import com.sidelined.backend.exception.AuthServiceUnavailableException;
import com.sidelined.backend.model.Credentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.nio.charset.StandardCharsets;

// The only code that calls Supabase Auth. The frontend talks to our /auth endpoints instead, so the
// anon key and project URL stay on the server.
@Service
public class SupabaseAuthClient {

    private final RestClient restClient;

    @Autowired
    public SupabaseAuthClient(@Value("${supabase.url}") String supabaseUrl,
                              @Value("${supabase.anon-key}") String anonKey) {
        this(RestClient.builder(), supabaseUrl, anonKey);
    }

    // Tests pass a builder bound to a mock server.
    SupabaseAuthClient(RestClient.Builder builder, String supabaseUrl, String anonKey) {
        this.restClient = builder
            .baseUrl(supabaseUrl + "/auth/v1")
            .defaultHeader("apikey", anonKey)
            .build();
    }

    // Creates the user in Supabase (which hashes and stores the password).
    public ResponseEntity<String> signUp(Credentials credentials) {
        return forward("/signup", credentials);
    }

    // Checks the email and password; on success Supabase's response includes access_token.
    public ResponseEntity<String> signIn(Credentials credentials) {
        return forward("/token?grant_type=password", credentials);
    }

    // Supabase's status and body come back unchanged, errors included ("Invalid login credentials"
    // is a 400), so the frontend can show Supabase's message.
    private ResponseEntity<String> forward(String path, Credentials credentials) {
        try {
            return restClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(credentials)
                .exchange((request, response) -> {
                    MediaType type = response.getHeaders().getContentType();
                    return ResponseEntity.status(response.getStatusCode())
                        .contentType(type != null ? type : MediaType.APPLICATION_JSON)
                        .body(new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
                });
        } catch (RestClientException e) {
            throw new AuthServiceUnavailableException(e);
        }
    }
}
