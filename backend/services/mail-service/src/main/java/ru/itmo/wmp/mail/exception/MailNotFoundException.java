package ru.itmo.wmp.mail.exception;

public class MailNotFoundException extends RuntimeException {
    public MailNotFoundException(String messageId) {
        super("Mail with messageId=" + messageId + " not found");
    }
}
