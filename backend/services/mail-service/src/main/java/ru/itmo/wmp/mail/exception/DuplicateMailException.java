package ru.itmo.wmp.mail.exception;

public class DuplicateMailException extends RuntimeException {
    public DuplicateMailException(String messageId) {
        super("Mail with messageId=" + messageId + " already exists");
    }
}
