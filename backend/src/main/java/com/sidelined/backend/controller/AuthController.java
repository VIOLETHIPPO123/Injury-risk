package com.sidelined.backend.controller;

import com.sidelined.backend.model.Credentials;
import com.sidelined.backend.service.SupabaseAuthClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Sign-up and sign-in, forwarded to Supabase. Both are public; see SecurityConfig.
// Body: { "email": "...", "password": "..." }.
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final SupabaseAuthClient supabaseAuthClient;

    public AuthController(SupabaseAuthClient supabaseAuthClient) {
        this.supabaseAuthClient = supabaseAuthClient;
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@RequestBody Credentials credentials) {
        return supabaseAuthClient.signUp(credentials);
    }

    // Supabase's response, including access_token, for the frontend to send as "Bearer <token>".
    @PostMapping("/signin")
    public ResponseEntity<String> signIn(@RequestBody Credentials credentials) {
        return supabaseAuthClient.signIn(credentials);
    }
}
