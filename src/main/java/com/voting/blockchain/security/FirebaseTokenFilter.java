// src/main/java/com/voting/blockchain/security/FirebaseTokenFilter.java
package com.voting.blockchain.security;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.voting.blockchain.service.FirebaseAuthenticationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

public class FirebaseTokenFilter extends OncePerRequestFilter {

    private final FirebaseAuthenticationService firebaseAuthService;

    public FirebaseTokenFilter(FirebaseAuthenticationService firebaseAuthService) {
        this.firebaseAuthService = firebaseAuthService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String idToken = authorizationHeader.substring(7); // Extract the token

            try {
                FirebaseToken firebaseToken = firebaseAuthService.verifyToken(idToken);

                UserDetails userDetails = User.builder()
                        .username(firebaseToken.getUid()) // Firebase UID as username
                        .password("") // Password not needed for token-based auth
                        .authorities(Collections.emptyList()) // No specific roles for now
                        .build();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);

                System.out.println("Firebase Token verified for UID: " + firebaseToken.getUid());

            } catch (FirebaseAuthException e) {
                System.err.println("Firebase Token verification failed: " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized
                response.getWriter().write("{\"error\": \"Unauthorized: Invalid or expired token.\"}");
                return;
            } catch (IllegalArgumentException e) {
                System.err.println("Missing or malformed Authorization header: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}