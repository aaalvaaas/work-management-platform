package ru.itmo.wmp.platform.exception.mailbox;

public class SecretCipherException extends RuntimeException {
    public SecretCipherException(String message) {
        super(message);
    }

    public SecretCipherException(String message, Throwable cause) {
        super(message, cause);
    }
}
