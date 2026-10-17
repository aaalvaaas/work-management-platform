package ru.itmo.wmp.platform.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.itmo.wmp.platform.application.security.SecretCipher;
import ru.itmo.wmp.platform.config.properties.EncryptionProperties;
import ru.itmo.wmp.platform.exception.mailbox.SecretCipherException;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class AesGcmSecretCipher implements SecretCipher {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final EncryptionProperties encryptionProperties;
    private final SecureRandom random = new SecureRandom();

    @Override
    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }

        byte[] iv = new byte[IV_LENGTH];

        random.nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] result = new byte[iv.length + encrypted.length];

            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(result);
        } catch (GeneralSecurityException exception) {
            throw new SecretCipherException("Unable to encrypt the secret", exception);
        }
    }

    @Override
    public String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }

        byte[] decoded = Base64.getDecoder().decode(cipherText);

        if (decoded.length <= IV_LENGTH) {
            throw new SecretCipherException("Stored secret is too short to be valid");
        }

        byte[] iv = new byte[IV_LENGTH];
        byte[] encrypted = new byte[decoded.length - IV_LENGTH];

        System.arraycopy(decoded, 0, iv, 0, IV_LENGTH);
        System.arraycopy(decoded, IV_LENGTH, encrypted, 0, encrypted.length);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new SecretCipherException("Unable to read the stored secret", exception);
        }
    }

    private SecretKeySpec key() {
        byte[] key = Base64.getDecoder().decode(encryptionProperties.key());

        if (key.length != 16 && key.length != 24 && key.length != 32) {
            throw new SecretCipherException(
                "Encryption key must be 16, 24 or 32 bytes, got " + key.length
            );
        }

        return new SecretKeySpec(key, ALGORITHM);
    }
}
