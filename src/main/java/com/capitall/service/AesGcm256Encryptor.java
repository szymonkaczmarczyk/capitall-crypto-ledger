package com.capitall.service;

import com.capitall.exception.EncryptionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

@Service
public class AesGcm256Encryptor implements ApiSecretsEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;

    private static final String V2_PREFIX = "v2:";
    private static final int PBKDF2_ITERATIONS = 100_000;
    private static final int PBKDF2_KEY_BITS = 256;

    private final SecretKeySpec legacyKey;
    private final SecretKeySpec primaryKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcm256Encryptor(
            @Value("${capitall.encryption.secret-key}") String secretKey,
            @Value("${capitall.encryption.salt:capitall-static-salt-v1}") String salt) {
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Encryption key must not be null or empty");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            this.legacyKey = new SecretKeySpec(digest.digest(secretKey.getBytes(StandardCharsets.UTF_8)), "AES");

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(secretKey.toCharArray(),
                    salt.getBytes(StandardCharsets.UTF_8), PBKDF2_ITERATIONS, PBKDF2_KEY_BITS);
            SecretKey derived = factory.generateSecret(spec);
            this.primaryKey = new SecretKeySpec(derived.getEncoded(), "AES");
        } catch (NoSuchAlgorithmException | java.security.spec.InvalidKeySpecException e) {
            throw new IllegalStateException("Failed to initialize key derivation", e);
        }
    }

    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTE];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, primaryKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return V2_PREFIX + Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new EncryptionException("Error encrypting text", e);
        }
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null) {
            return null;
        }
        try {
            SecretKeySpec key;
            String payload;
            if (encryptedText.startsWith(V2_PREFIX)) {
                key = primaryKey;
                payload = encryptedText.substring(V2_PREFIX.length());
            } else {
                key = legacyKey;
                payload = encryptedText;
            }

            byte[] encryptedBytes = Base64.getDecoder().decode(payload);
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedBytes);
            byte[] iv = new byte[IV_LENGTH_BYTE];
            byteBuffer.get(iv);
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            byte[] plainTextBytes = cipher.doFinal(cipherText);
            return new String(plainTextBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new EncryptionException("Error decrypting text", e);
        }
    }
}
