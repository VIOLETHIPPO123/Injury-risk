package com.sidelined.backend.controller;

import com.sidelined.backend.model.Credentials;
import com.sidelined.backend.service.GoogleSignIn;
import com.sidelined.backend.service.SupabaseAuthClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Sign-up and sign-in, forwarded to Supabase, and the start of Google sign-in. All are public;
// see SecurityConfig. Body for sign-up and sign-in: { "email": "...", "password": "..." }.
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final SupabaseAuthClient supabaseAuthClient;
    private final GoogleSignIn googleSignIn;

    public AuthController(SupabaseAuthClient supabaseAuthClient, GoogleSignIn googleSignIn) {
        this.supabaseAuthClient = supabaseAuthClient;
        this.googleSignIn = googleSignIn;
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

    // The "Sign in with Google" button links here; the browser is sent on to Supabase and Google.
    // A Google user then gets their profile through GET /me, exactly like an email user.
    @GetMapping("/google")
    public ResponseEntity<Void> signInWithGoogle() {
        return ResponseEntity.status(HttpStatus.FOUND).location(googleSignIn.authorizeUri()).build();
    }
}
