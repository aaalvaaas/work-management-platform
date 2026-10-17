package ru.itmo.wmp.platform.exception.mailbox;

public class MailboxNotFoundException extends RuntimeException {
    public MailboxNotFoundException(Long organizationId) {
        super("Organization id=" + organizationId + " has no mailbox connected");
    }
}
