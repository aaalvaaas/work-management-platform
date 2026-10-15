package ru.itmo.wmp.platform.exception.registration;

public class EmailAlreadyUsedException extends RuntimeException {
    public EmailAlreadyUsedException(String email) {
        super("Email " + email + " is already used by another account");
    }
}
