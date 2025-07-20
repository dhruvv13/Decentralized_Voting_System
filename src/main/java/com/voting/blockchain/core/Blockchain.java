package com.voting.blockchain.core;
import com.voting.blockchain.util.CryptoUtil;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;


import java.io.IOException;
import com.fasterxml.jackson.databind.JsonMappingException;


import com.google.cloud.firestore.Firestore; // Import Firestore
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.WriteBatch;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.api.core.ApiFutures; // For handling Futures
import com.voting.blockchain.model.Block;
import com.voting.blockchain.model.VoteTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.MessageDigest; // Ensure this is imported for calculateHash
import java.security.NoSuchAlgorithmException; // Ensure this is imported
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException; // Ensure this is imported


@Component
public class Blockchain {
    private List<Block> chain;
    public int difficulty;
    private List<VoteTransaction> pendingTransactions;

    private final Firestore firestore;
    private final ObjectMapper objectMapper;

    private static final String APP_ID = "decentralized-voting-app"; // You can change this
    private static final String BLOCKS_COLLECTION_PATH = "artifacts/" + APP_ID + "/public/data/blocks";
    private static final String PENDING_TX_COLLECTION_PATH = "artifacts/" + APP_ID + "/public/data/pendingTransactions";


    @Autowired
    public Blockchain(Firestore firestore) {
        this.firestore = firestore;
        this.objectMapper = new ObjectMapper();
        this.chain = new ArrayList<>();
        this.difficulty = 4;
        this.pendingTransactions = Collections.synchronizedList(new ArrayList<>());

        loadBlockchainFromFirestore();
    }

    private void loadBlockchainFromFirestore() {
        System.out.println("Attempting to load blockchain from Firestore...");
        try {
            // Load Blocks
            List<QueryDocumentSnapshot> blockDocuments = firestore.collection(BLOCKS_COLLECTION_PATH)
                    .orderBy("index")
                    .get()
                    .get()
                    .getDocuments();

            if (!blockDocuments.isEmpty()) {
                for (QueryDocumentSnapshot document : blockDocuments) {
                    String dataJson = (String) document.get("dataJson");
                    List<VoteTransaction> transactions = objectMapper.readValue(dataJson, new TypeReference<List<VoteTransaction>>() {});

                    Block loadedBlock = new Block(
                            document.getLong("index").intValue(),
                            document.getString("previousHash"),
                            transactions
                    );
                    loadedBlock.setHash(document.getString("hash"));
                    loadedBlock.setNonce(document.getLong("nonce").intValue());
                    loadedBlock.setTimestamp(document.getLong("timestamp"));

                    chain.add(loadedBlock);
                }
                System.out.println("Blockchain loaded from Firestore. Chain length: " + chain.size());
            } else {
                System.out.println("No existing blockchain found in Firestore. Creating genesis block.");
            }

            // Load Pending Transactions
            List<QueryDocumentSnapshot> pendingTxDocuments = firestore.collection(PENDING_TX_COLLECTION_PATH)
                    .get()
                    .get()
                    .getDocuments();

            if (!pendingTxDocuments.isEmpty()) {
                for (QueryDocumentSnapshot document : pendingTxDocuments) {
                    pendingTransactions.add(document.toObject(VoteTransaction.class));
                }
                System.out.println("Pending transactions loaded from Firestore. Count: " + pendingTransactions.size());
            }

        } catch (InterruptedException | ExecutionException | IOException e) {
            System.err.println("Error loading blockchain from Firestore: " + e.getMessage());
        }

        // Ensure genesis block is created if chain is still empty after load (e.g., first run or load failed)
        if (chain.isEmpty()) {
            createGenesisBlock();
        }
    }

    private void createGenesisBlock() {
        List<VoteTransaction> genesisData = new ArrayList<>();
        genesisData.add(new VoteTransaction("system", "genesis_block_creation" , "",""));
        Block genesisBlock = new Block(0, "0", genesisData);
        mineBlock(genesisBlock);
        chain.add(genesisBlock);
        saveBlockToFirestore(genesisBlock);
        System.out.println("Genesis Block created and saved: " + genesisBlock.getHash());
    }

    public Block getLatestBlock() {
        if (chain.isEmpty()) {
            throw new IllegalStateException("Blockchain is empty, genesis block not created.");
        }
        return chain.get(chain.size() - 1);
    }

    public void addTransaction(VoteTransaction transaction) {
        if (transaction == null || transaction.getVoterId() == null || transaction.getCandidateId() == null) {
            throw new IllegalArgumentException("Invalid transaction data. Voter ID and Candidate ID are required.");
        }
        this.pendingTransactions.add(transaction);
        savePendingTransactionToFirestore(transaction);
        System.out.println("Transaction added to pending: " + transaction.toString());
    }

    public Block minePendingTransactions() {
        if (pendingTransactions.isEmpty()) {
            System.out.println("No pending transactions to mine.");
            return null;
        }

        Block newBlock = new Block(
                chain.size(),
                getLatestBlock().getHash(),
                new ArrayList<>(this.pendingTransactions)
        );

        mineBlock(newBlock);
        chain.add(newBlock);
        saveBlockToFirestore(newBlock);

        clearPendingTransactionsInFirestore();
        this.pendingTransactions.clear();
        System.out.println("New Block mined, added, and saved: " + newBlock.getHash());
        return newBlock;
    }

    private void mineBlock(Block block) {
        String targetPrefix = new String(new char[difficulty]).replace('\0', '0');
        while (!block.getHash().substring(0, difficulty).equals(targetPrefix)) {
            block.setNonce(block.getNonce() + 1);
            block.setHash(block.calculateHash());
        }
        System.out.println("Block Mined: " + block.getHash() + " (nonce: " + block.getNonce() + ")");
    }

    public boolean isChainValid() {
        for (int i = 1; i < chain.size(); i++) {
            Block currentBlock = chain.get(i);
            Block previousBlock = chain.get(i - 1);

            if (!currentBlock.getHash().equals(currentBlock.calculateHash())) {
                System.out.println("Chain Invalid: Current block hash mismatch at index " + currentBlock.getIndex());
                return false;
            }

            if (!currentBlock.getPreviousHash().equals(previousBlock.getHash())) {
                System.out.println("Chain Invalid: Previous hash mismatch at index " + currentBlock.getIndex());
                return false;
            }

            String targetPrefix = new String(new char[difficulty]).replace('\0', '0');
            if (!currentBlock.getHash().substring(0, difficulty).equals(targetPrefix)) {
                System.out.println("Chain Invalid: Block hash does not meet difficulty at index " + currentBlock.getIndex());
                return false;
            }
        }
        return true;
    }

    public List<Block> getChain() {
        return chain;
    }

    public List<VoteTransaction> getPendingTransactions() {
        return pendingTransactions;
    }

    // --- Firestore Helper Methods ---

    private void saveBlockToFirestore(Block block) {
        try {
            String dataJson = objectMapper.writeValueAsString(block.getData());

            java.util.Map<String, Object> blockMap = new java.util.HashMap<>();
            blockMap.put("index", block.getIndex());
            blockMap.put("timestamp", block.getTimestamp());
            blockMap.put("previousHash", block.getPreviousHash());
            blockMap.put("hash", block.getHash());
            blockMap.put("nonce", block.getNonce());
            blockMap.put("dataJson", dataJson); // Store transactions as JSON string

            firestore.collection(BLOCKS_COLLECTION_PATH)
                    .document(String.valueOf(block.getIndex()))
                    .set(blockMap)
                    .get();
            System.out.println("Block " + block.getIndex() + " saved to Firestore.");
        } catch (InterruptedException | ExecutionException | JsonProcessingException e) { // Catch JsonProcessingException
            System.err.println("Error saving block to Firestore: " + e.getMessage());
        }
    }

    private void savePendingTransactionToFirestore(VoteTransaction transaction) {
        try {
            firestore.collection(PENDING_TX_COLLECTION_PATH)
                    .add(transaction)
                    .get();
            System.out.println("Pending transaction saved to Firestore.");
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error saving pending transaction to Firestore: " + e.getMessage());
        }
    }

    private void clearPendingTransactionsInFirestore() {
        try {
            WriteBatch batch = firestore.batch();
            firestore.collection(PENDING_TX_COLLECTION_PATH)
                    .get()
                    .get()
                    .getDocuments()
                    .forEach(doc -> batch.delete(doc.getReference()));
            batch.commit().get();
            System.out.println("Cleared all pending transactions from Firestore.");
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error clearing pending transactions from Firestore: " + e.getMessage());
        }
    }
}