// src/main/java/com/voting/blockchain/controller/BlockchainController.java
package com.voting.blockchain.controller;

import com.voting.blockchain.core.Blockchain;
import com.voting.blockchain.model.Block;
import com.voting.blockchain.model.VoteTransaction;
import com.voting.blockchain.service.FirebaseAuthenticationService;
import com.voting.blockchain.util.CryptoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails; // NEW: Import UserDetails (CRUCIAL FIX)
import com.google.firebase.auth.FirebaseAuthException;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class BlockchainController {

    private final Blockchain blockchain;
    private final FirebaseAuthenticationService firebaseAuthService;

    @Autowired
    public BlockchainController(Blockchain blockchain, FirebaseAuthenticationService firebaseAuthService) {
        this.blockchain = blockchain;
        this.firebaseAuthService = firebaseAuthService;
    }

    /**
     * Endpoint to get the full blockchain.
     * GET /api/v1/blockchain
     * @return List of all blocks in the chain
     */
    @GetMapping("/blockchain")
    public ResponseEntity<Map<String, Object>> getFullChain() {
        Map<String, Object> response = new HashMap<>();
        response.put("chain", blockchain.getChain());
        response.put("length", blockchain.getChain().size());
        response.put("isValid", blockchain.isChainValid()); // Also report if the chain is valid
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to generate a new RSA cryptographic key pair.
     * FOR TESTING/DEMO PURPOSES ONLY. In a real application, private keys
     * are generated and securely stored client-side, never transmitted.
     * GET /api/v1/generateKeys
     * @return A map containing encoded public and private keys.
     */
    @GetMapping("/generateKeys")
    public ResponseEntity<Map<String, String>> generateKeyPair() {
        try {
            KeyPair keyPair = CryptoUtil.generateKeyPair();
            String publicKey = CryptoUtil.encodePublicKey(keyPair.getPublic());
            String privateKey = CryptoUtil.encodePrivateKey(keyPair.getPrivate());

            Map<String, String> keys = new HashMap<>();
            keys.put("publicKey", publicKey);
            keys.put("privateKey", privateKey); // WARNING: Never expose private key in real app!
            keys.put("message", "Generated key pair. Keep your privateKey secret!");

            return ResponseEntity.ok(keys);
        } catch (NoSuchAlgorithmException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Key generation failed: " + e.getMessage()));
        }
    }

    // TEMPORARILY COMMENTED OUT THIS METHOD TO RESOLVE COMPILATION ERROR
    /*
    @PostMapping("/transactions/register")
    public ResponseEntity<Map<String, String>> registerUser(@RequestBody Map<String, String> registrationRequest) {
        String email = registrationRequest.get("email");
        String password = registrationRequest.get("password");

        if (email == null || password == null || email.isEmpty() || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Email and password are required."));
        }

        try {
            String uid = firebaseAuthService.createUser(email, password);
            return ResponseEntity.status(HttpStatus.CREATED)
                                 .body(Collections.singletonMap("message", "User registered successfully. UID: " + uid));
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(Collections.singletonMap("error", "Firebase Auth Error: " + e.getMessage()));
        }
    }
    */

    /**
     * Endpoint to add a new vote transaction.
     * Voter ID is now taken from the authenticated Firebase user's UID.
     * This endpoint requires authentication (Authorization: Bearer <Firebase_ID_Token>).
     * POST /api/v1/transactions/new
     * Request Body: { "candidateId": "...", "senderPublicKey": "...", "signature": "..." }
     * @param transactionRequest The request containing candidate ID, public key, and signature
     * @return Confirmation message
     */
    @PostMapping("/transactions/new")
    public ResponseEntity<Map<String, String>> newTransaction(@RequestBody Map<String, String> transactionRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "Authentication required to cast a vote."));
        }

        // --- FIX FOR ClassCastException (CRUCIAL) ---
        UserDetails userDetails = (UserDetails) authentication.getPrincipal(); // Correctly cast to UserDetails
        String voterId = userDetails.getUsername(); // Get the UID from the UserDetails object's username property
        // --- END FIX ---

        String candidateId = transactionRequest.get("candidateId");
        String senderPublicKey = transactionRequest.get("senderPublicKey");
        String signature = transactionRequest.get("signature");

        if (candidateId == null || candidateId.isEmpty() ||
                senderPublicKey == null || senderPublicKey.isEmpty() ||
                signature == null || signature.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Candidate ID, senderPublicKey, and signature are required."));
        }

        // Create the VoteTransaction with the authenticated voterId AND signature data
        VoteTransaction transaction = new VoteTransaction(voterId, candidateId, senderPublicKey, signature);

        try {
            blockchain.addTransaction(transaction); // This will now verify the signature
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Collections.singletonMap("message", "Transaction will be added to Block " + (blockchain.getChain().size() + 1) + " by voter: " + voterId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Transaction verification failed: " + e.getMessage()));
        }
    }

    /**
     * Endpoint to mine a new block. This processes all pending transactions.
     * GET /api/v1/mine
     * @return Details of the new block and pending transactions
     */
    @GetMapping("/mine")
    public ResponseEntity<Map<String, Object>> mineBlock() {
        Block newBlock = blockchain.minePendingTransactions();

        if (newBlock == null) {
            return ResponseEntity.ok(Collections.singletonMap("message", "No pending transactions to mine."));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "New Block Forged!");
        response.put("block", newBlock);
        response.put("pendingTransactionsAfterMine", blockchain.getPendingTransactions()); // Should be empty
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to get pending transactions.
     * GET /api/v1/transactions/pending
     * @return List of pending vote transactions
     */
    @GetMapping("/transactions/pending")
    public ResponseEntity<Map<String, Object>> getPendingTransactions() {
        Map<String, Object> response = new HashMap<>();
        response.put("pending_transactions", blockchain.getPendingTransactions());
        response.put("count", blockchain.getPendingTransactions().size());
        return ResponseEntity.ok(response);
    }
}