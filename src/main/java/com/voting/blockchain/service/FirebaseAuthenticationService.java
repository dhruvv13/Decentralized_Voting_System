// src/main/java/com/voting/blockchain/service/FirebaseAuthenticationService.java
package com.voting.blockchain.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException; // Not strictly needed for these methods, but often comes with Firebase futures

@Service // Marks this as a Spring service component
public class FirebaseAuthenticationService {

    // Method to verify a Firebase ID token
    public FirebaseToken verifyToken(String idToken) throws FirebaseAuthException {
        if (idToken == null || idToken.isEmpty()) {
            throw new IllegalArgumentException("ID Token cannot be null or empty.");
        }
        // Verify the ID token, checking if it's expired or revoked.
        return FirebaseAuth.getInstance().verifyIdToken(idToken);
    }

    // Method to get a user by their UID
    public String getUserEmailByUid(String uid) throws FirebaseAuthException {
        return FirebaseAuth.getInstance().getUser(uid).getEmail();
    }

    // Method to create a new user in Firebase Auth
    // In a a real app, this would be a separate registration endpoint,
    // and probably handled by a frontend submitting email/password.

    /*public String createUser(String email, String password) throws FirebaseAuthException {
        return FirebaseAuth.getInstance().createUser(new com.google.firebase.auth.UserRecord.Builder()
                .setEmail(email)
                .setPassword(password)
                .build()).getUid();
    }
    */


}