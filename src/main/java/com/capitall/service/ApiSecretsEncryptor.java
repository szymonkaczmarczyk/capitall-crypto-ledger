package com.capitall.service;

public interface ApiSecretsEncryptor {
    String encrypt(String plainText);
    String decrypt(String encryptedText);
}
