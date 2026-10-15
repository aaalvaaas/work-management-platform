package ru.itmo.wmp.platform.exception.registration;

public class InvitationNotValidException extends RuntimeException {
    public InvitationNotValidException() {
        super("Invitation is invalid, expired or already used");
    }
}
