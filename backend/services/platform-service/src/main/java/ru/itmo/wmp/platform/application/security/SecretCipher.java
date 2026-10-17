package ru.itmo.wmp.platform.application.security;

public interface SecretCipher {
    String encrypt(String plainText);

    String decrypt(String cipherText);
}
