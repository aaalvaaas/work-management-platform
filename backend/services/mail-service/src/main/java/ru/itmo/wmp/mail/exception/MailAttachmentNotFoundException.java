package ru.itmo.wmp.mail.exception;

public class MailAttachmentNotFoundException extends RuntimeException {
    public MailAttachmentNotFoundException(Long attachmentId) {
        super("Mail attachment not found: " + attachmentId);
    }
}
