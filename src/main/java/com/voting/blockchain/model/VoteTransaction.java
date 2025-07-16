// src/main/java/com/voting/blockchain/model/VoteTransaction.java
package com.voting.blockchain.model;

// If you're using Lombok, you can add @NoArgsConstructor and @AllArgsConstructor
// import lombok.NoArgsConstructor;
// import lombok.AllArgsConstructor;

// @NoArgsConstructor // Add this if using Lombok
// @AllArgsConstructor // Add this if using Lombok
public class VoteTransaction {
    private String voterId;
    private String candidateId;
    private long timestamp;
    // Add more fields later, like signature, transaction ID, etc.

    // ADD THIS NEW CONSTRUCTOR (THIS IS THE CRITICAL PART)
    public VoteTransaction() {
        // Default constructor needed by Firestore/Jackson for deserialization
    }

    public VoteTransaction(String voterId, String candidateId) {
        this.voterId = voterId;
        this.candidateId = candidateId;
        this.timestamp = System.currentTimeMillis();
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

    // --- Setters (needed for Firestore/Jackson mapping) ---
    public void setVoterId(String voterId) {
        this.voterId = voterId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "VoteTransaction{" +
                "voterId='" + voterId + '\'' +
                ", candidateId='" + candidateId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}