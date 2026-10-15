package ru.itmo.wmp.platform.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Invalid ISU number or password");
    }
}
