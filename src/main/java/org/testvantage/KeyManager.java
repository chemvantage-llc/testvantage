package org.testvantage;

import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Base64;

/**
 * Manages RSA key pairs for signing JWTs
 * In production, keys should be persisted and rotated periodically
 */
public class KeyManager {
    
    private static KeyManager instance;
    private KeyPair keyPair;
    private String keyId;
    
    private KeyManager() {
        generateKeyPair();
    }
    
    public static synchronized KeyManager getInstance() {
        if (instance == null) {
            instance = new KeyManager();
        }
        return instance;
    }
    
    private void generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4));
            this.keyPair = keyGen.generateKeyPair();
            this.keyId = "test-vantage-key-" + System.currentTimeMillis();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate key pair", e);
        }
    }
    
    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }
    
    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }
    
    public String getKeyId() {
        return keyId;
    }
    
    /**
     * Get the public key modulus (n) in Base64url encoding
     */
    public String getPublicKeyModulus() {
        RSAPublicKey rsaPublicKey = (RSAPublicKey) keyPair.getPublic();
        BigInteger modulus = rsaPublicKey.getModulus();
        byte[] modulusBytes = modulus.toByteArray();
        
        // Remove leading zero byte if present
        if (modulusBytes[0] == 0) {
            byte[] tmp = new byte[modulusBytes.length - 1];
            System.arraycopy(modulusBytes, 1, tmp, 0, tmp.length);
            modulusBytes = tmp;
        }
        
        return Base64.getUrlEncoder().withoutPadding().encodeToString(modulusBytes);
    }
    
    /**
     * Get the public key exponent (e) in Base64url encoding
     */
    public String getPublicKeyExponent() {
        RSAPublicKey rsaPublicKey = (RSAPublicKey) keyPair.getPublic();
        BigInteger exponent = rsaPublicKey.getPublicExponent();
        byte[] exponentBytes = exponent.toByteArray();
        
        // Remove leading zero byte if present
        if (exponentBytes[0] == 0) {
            byte[] tmp = new byte[exponentBytes.length - 1];
            System.arraycopy(exponentBytes, 1, tmp, 0, tmp.length);
            exponentBytes = tmp;
        }
        
        return Base64.getUrlEncoder().withoutPadding().encodeToString(exponentBytes);
    }
    
    /**
     * Get the public key in PEM format
     */
    public String getPublicKeyPEM() {
        byte[] encoded = keyPair.getPublic().getEncoded();
        String base64 = Base64.getEncoder().encodeToString(encoded);
        
        StringBuilder pem = new StringBuilder();
        pem.append("-----BEGIN PUBLIC KEY-----\n");
        
        int index = 0;
        while (index < base64.length()) {
            pem.append(base64, index, Math.min(index + 64, base64.length()));
            pem.append("\n");
            index += 64;
        }
        
        pem.append("-----END PUBLIC KEY-----\n");
        return pem.toString();
    }
}
