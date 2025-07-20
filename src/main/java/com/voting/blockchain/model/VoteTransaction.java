// src/main/java/com/voting/blockchain/model/VoteTransaction.java
package com.voting.blockchain.model;

// Ensure Lombok annotations are commented out or removed if they cause conflicts with explicit constructors
// import lombok.NoArgsConstructor;
// import lombok.AllArgsConstructor;

public class VoteTransaction {
    private String voterId;       // Firebase UID or unique voter identifier
    private String candidateId;
    private long timestamp;
    private String senderPublicKey; // NEW: The public key (encoded as String) of the voter
    private String signature;       // NEW: The digital signature (encoded as String) of this transaction

    // No-argument constructor needed by Firestore/Jackson for deserialization
    public VoteTransaction() {
        // Default constructor for Firestore
    }

    // Updated constructor to include senderPublicKey and signature
    public VoteTransaction(String voterId, String candidateId, String senderPublicKey, String signature) {
        this.voterId = voterId;
        this.candidateId = candidateId;
        this.timestamp = System.currentTimeMillis();
        this.senderPublicKey = senderPublicKey; // Initialize new fields
        this.signature = signature;
    }

    // --- Getters ---
    public String getVoterId() {
        return voterId;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // NEW Getters for new fields
    public String getSenderPublicKey() {
        return senderPublicKey;
    }

    public String getSignature() {
        return signature;
    }

    // --- Setters (needed for Firestore POJO mapping for all fields) ---
    public void setVoterId(String voterId) {
        this.voterId = voterId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // NEW Setters for new fields
    public void setSenderPublicKey(String senderPublicKey) {
        this.senderPublicKey = senderPublicKey;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    // --- Helper method to get the data that will be signed ---
    // This string represents the immutable data that will be signed
    // Order of concatenation is important and must be consistent for signing/verification
    public String calculateSignatureData() {
        return voterId + candidateId + senderPublicKey;
    }


    @Override
    public String toString() {
        return "VoteTransaction{" +
                "voterId='" + voterId + '\'' +
                ", candidateId='" + candidateId + '\'' +
                ", timestamp=" + timestamp +
                ", senderPublicKey='" + senderPublicKey + '\'' +
                ", signature='" + signature + '\'' +
                '}';
    }
}