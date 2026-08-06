package ru.itmo.wmp.mail.exception;

public class MailImportException extends RuntimeException {
    public MailImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
