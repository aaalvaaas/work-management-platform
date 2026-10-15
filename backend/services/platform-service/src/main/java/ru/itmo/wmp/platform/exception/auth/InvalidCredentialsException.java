package ru.itmo.wmp.platform.exception.auth;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Invalid ISU number or password");
    }
}
