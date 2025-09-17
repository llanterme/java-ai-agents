package za.co.digitalcowboy.agents.service.oauth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class TokenEncryptionService {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenEncryptionService.class);
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    
    @Value("${oauth.encryption.key:}")
    private String encryptionKeyBase64;
    
    private SecretKey getSecretKey() {
        if (encryptionKeyBase64 == null || encryptionKeyBase64.trim().isEmpty()) {
            logger.warn("No encryption key configured. Generating a temporary key for this session.");
            // Generate a temporary key - in production this should come from configuration
            try {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
                keyGenerator.init(256);
                return keyGenerator.generateKey();
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate temporary encryption key", e);
            }
        }
        
        try {
            byte[] decodedKey = Base64.getDecoder().decode(encryptionKeyBase64);
            return new SecretKeySpec(decodedKey, ALGORITHM);
        } catch (Exception e) {
            throw new RuntimeException("Invalid encryption key configuration", e);
        }
    }
    
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        
        try {
            SecretKey secretKey = getSecretKey();
            
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
            
            byte[] encryptedData = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            
            // Combine IV and encrypted data
            byte[] result = new byte[GCM_IV_LENGTH + encryptedData.length];
            System.arraycopy(iv, 0, result, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedData, 0, result, GCM_IV_LENGTH, encryptedData.length);
            
            return Base64.getEncoder().encodeToString(result);
            
        } catch (Exception e) {
            logger.error("Failed to encrypt token", e);
            throw new RuntimeException("Token encryption failed", e);
        }
    }
    
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        
        try {
            SecretKey secretKey = getSecretKey();
            
            byte[] decodedData = Base64.getDecoder().decode(encryptedText);
            
            // Extract IV and encrypted data
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encryptedData = new byte[decodedData.length - GCM_IV_LENGTH];
            System.arraycopy(decodedData, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(decodedData, GCM_IV_LENGTH, encryptedData, 0, encryptedData.length);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
            
            byte[] decryptedData = cipher.doFinal(encryptedData);
            
            return new String(decryptedData, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            logger.error("Failed to decrypt token", e);
            throw new RuntimeException("Token decryption failed", e);
        }
    }
}