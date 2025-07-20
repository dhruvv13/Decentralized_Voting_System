// src/main/java/com/voting/blockchain/util/CryptoUtil.java
package com.voting.blockchain.util;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64; // For Base64 encoding/decoding

public class CryptoUtil {

    // --- Key Pair Generation ---
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA"); // Using RSA algorithm
        keyGen.initialize(2048); // Key size (2048 bits recommended)
        return keyGen.generateKeyPair();
    }

    // --- Signing Data ---
    // Signs a string of data using the private key
    public static String sign(PrivateKey privateKey, String data) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature rsa = Signature.getInstance("SHA256withRSA"); // Algorithm for signing
        rsa.initSign(privateKey);
        rsa.update(data.getBytes());
        return Base64.getEncoder().encodeToString(rsa.sign());
    }

    // --- Verifying Signature ---
    // Verifies a signature against the original data and the public key
    public static boolean verify(PublicKey publicKey, String data, String signature) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature rsa = Signature.getInstance("SHA256withRSA");
        rsa.initVerify(publicKey);
        rsa.update(data.getBytes());
        return rsa.verify(Base64.getDecoder().decode(signature));
    }

    // --- Key Conversion Utilities (for storing/transmitting keys as strings) ---
    // Encodes a public key to a Base64 string
    public static String encodePublicKey(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    // Decodes a Base64 string back to a PublicKey object
    public static PublicKey decodePublicKey(String publicKeyEncoded) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] publicBytes = Base64.getDecoder().decode(publicKeyEncoded);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    // Encodes a private key to a Base64 string
    public static String encodePrivateKey(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    // Decodes a Base64 string back to a PrivateKey object
    public static PrivateKey decodePrivateKey(String privateKeyEncoded) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] privateBytes = Base64.getDecoder().decode(privateKeyEncoded);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }
}