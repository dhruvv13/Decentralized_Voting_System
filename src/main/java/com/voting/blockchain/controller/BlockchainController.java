// src/main/java/com/voting/blockchain/controller/BlockchainController.java
package com.voting.blockchain.controller; // This must be the first line

import com.voting.blockchain.core.Blockchain; // Import from core package
import com.voting.blockchain.model.Block; // Import from model package
import com.voting.blockchain.model.VoteTransaction; // Import from model package
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController // Marks this class as a Spring REST Controller
@RequestMapping("/api/v1") // Base path for all endpoints in this controller
public class BlockchainController {

    private final Blockchain blockchain; // Inject our Blockchain core component

    // Spring's @Autowired handles injecting the Blockchain instance
    @Autowired
    public BlockchainController(Blockchain blockchain) {
        this.blockchain = blockchain;
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
     * Endpoint to add a new vote transaction.
     * POST /api/v1/transactions/new
     * Request Body: { "voterId": "...", "candidateId": "..." }
     * @param transaction The vote transaction to add
     * @return Confirmation message
     */
    @PostMapping("/transactions/new")
    public ResponseEntity<Map<String, String>> newTransaction(@RequestBody VoteTransaction transaction) {
        if (transaction == null || transaction.getVoterId() == null || transaction.getCandidateId() == null) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Invalid transaction data. Voter ID and Candidate ID are required."));
        }

        try {
            blockchain.addTransaction(transaction);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Collections.singletonMap("message", "Transaction will be added to Block " + (blockchain.getChain().size() + 1)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
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