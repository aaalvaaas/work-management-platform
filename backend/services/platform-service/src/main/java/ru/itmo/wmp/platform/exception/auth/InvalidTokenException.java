package ru.itmo.wmp.platform.exception.auth;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException() {
        super("Token is invalid, expired or already used");
    }
}
