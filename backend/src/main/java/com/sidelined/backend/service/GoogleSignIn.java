package com.sidelined.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;

// Where "Sign in with Google" starts: Supabase's authorize URL for its Google provider. It's built
// here so the frontend never needs the Supabase project URL. After Google, Supabase sends the browser
// back to the frontend with the session in the URL fragment (#access_token=...).
//
// The Google client id and secret live only in Supabase (Authentication > Providers > Google); this
// app never sees them. redirect_to is fixed here, not taken from the request, so this endpoint can't
// be used to send people anywhere else. It must also be in Supabase's Redirect URLs allow list.
@Component
public class GoogleSignIn {

    private final String supabaseUrl;
    private final String frontendUrl;

    public GoogleSignIn(@Value("${supabase.url}") String supabaseUrl,
                        @Value("${app.frontend-url}") String frontendUrl) {
        this.supabaseUrl = supabaseUrl;
        this.frontendUrl = frontendUrl;
    }

    public URI authorizeUri() {
        return UriComponentsBuilder.fromUriString(supabaseUrl)
            .path("/auth/v1/authorize")
            .queryParam("provider", "google")
            .queryParam("redirect_to", frontendUrl)
            .encode()
            .build()
            .toUri();
    }
}
