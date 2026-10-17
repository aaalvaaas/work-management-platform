package ru.itmo.wmp.mail.application.mailbox;

public record MailboxConnection(
    Long id,
    Long organizationId,
    String organizationCode,
    String host,
    Integer port,
    String username,
    String password,
    String folder,
    boolean useSsl
) {
}
