package com.sidelined.backend.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

// Who may call what. Requests carry a Supabase JWT as "Authorization: Bearer <token>"; Spring checks
// its signature against Supabase's public keys (application.properties) before any controller runs.
// A missing or bad token on a protected endpoint gets 401. Roles (403) are checked in ProfileService.
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            // The token travels in a header, not a cookie, so there is no CSRF risk and no session.
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET,
                    "/players", "/players/*/injuries", "/players/*/workload",
                    "/teams", "/roster/formats").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/signup", "/auth/signin").permitAll()
                // Lets error responses (e.g. a 400 for a bad path variable) through unchanged
                // instead of turning them into 401s.
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    // Moved here from the old CorsConfig, so CORS is applied before the token check: the browser's
    // preflight request carries no token. Authorization is allowed so the frontend can send it.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of("http://localhost:5173"));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
