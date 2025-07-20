// src/main/java/com/voting/blockchain/security/FirebaseSecurityConfig.java
package com.voting.blockchain.security;

import com.voting.blockchain.service.FirebaseAuthenticationService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity // Enables Spring Security's web security support
public class FirebaseSecurityConfig {

    private final FirebaseAuthenticationService firebaseAuthService;

    public FirebaseSecurityConfig(FirebaseAuthenticationService firebaseAuthService) {
        this.firebaseAuthService = firebaseAuthService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Disable CSRF for stateless APIs
        http.csrf(csrf -> csrf.disable());

        // Configure exception handling for unauthorized access
        http.exceptionHandling(exceptionHandling ->
                exceptionHandling.authenticationEntryPoint((request, response, authException) -> {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: " + authException.getMessage());
                }));

        // Configure session management to be stateless (no sessions created)
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Add our custom Firebase Token filter BEFORE the standard UsernamePasswordAuthenticationFilter
        // This filter will extract and verify the Firebase ID token from the "Authorization" header
        http.addFilterBefore(
                new FirebaseTokenFilter(firebaseAuthService),
                UsernamePasswordAuthenticationFilter.class
        );

        // Define authorization rules for API endpoints
        http.authorizeHttpRequests(authorize -> authorize
                // Allow unauthenticated access to /api/v1/blockchain and /api/v1/mine for viewing/mining publicly
                .requestMatchers("/api/v1/blockchain", "/api/v1/mine", "/api/v1/generateKeys").permitAll() // Added /generateKeys
                .requestMatchers("/api/v1/transactions/pending").permitAll() // Allow checking pending transactions publicly
                .requestMatchers("/api/v1/transactions/register").permitAll() // Allow new user registration without auth for now (temporary)

                // Require authentication for adding new transactions
                .requestMatchers("/api/v1/transactions/new").authenticated()

                // Deny all other requests by default (if not matched above)
                .anyRequest().denyAll()
        );

        return http.build();
    }
}