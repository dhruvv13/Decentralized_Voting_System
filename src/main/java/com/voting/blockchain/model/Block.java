// src/main/java/com/voting/blockchain/model/Block.java
package com.voting.blockchain.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;

public class Block {
    private int index;
    private long timestamp;
    private List<VoteTransaction> data; // List of vote transactions
    private String previousHash;
    private String hash;
    private int nonce; // Used for Proof-of-Work

    // Constructor for a new block
    public Block(int index, String previousHash, List<VoteTransaction> data) {
        this.index = index;
        this.timestamp = new Date().getTime();
        this.data = data;
        this.previousHash = previousHash;
        this.nonce = 0; // Initialize nonce
        this.hash = calculateHash(); // Calculate initial hash
    }

    // --- Getters ---
    public int getIndex() {
        return index;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public List<VoteTransaction> getData() {
        return data;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public String getHash() {
        return hash;
    }

    public int getNonce() {
        return nonce;
    }

    // --- Setters (for nonce and hash after mining) ---
    public void setNonce(int nonce) {
        this.nonce = nonce;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // Method to calculate the SHA-256 hash of the block's contents
    public String calculateHash() {
        // We'll use Jackson's ObjectMapper to convert the List<VoteTransaction> to a JSON string
        // This ensures consistent hashing regardless of object order or internal representation.
        ObjectMapper objectMapper = new ObjectMapper();
        String serializedData;
        try {
            serializedData = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            // Handle error, e.g., log it and return an empty string or throw a runtime exception
            System.err.println("Error serializing block data: " + e.getMessage());
            serializedData = ""; // Fallback
        }

        String dataToHash = index + Long.toString(timestamp) + serializedData + previousHash + nonce;
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            // This should ideally not happen for SHA-256
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
        byte[] hashBytes = digest.digest(dataToHash.getBytes());

        // Convert byte array to hexadecimal string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}