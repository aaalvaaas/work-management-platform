package ru.itmo.wmp.platform.application.mailbox;

public record MailboxSettings(
    String host,
    Integer port,
    String username,
    String password,
    String folder,
    boolean useSsl
) {
}
