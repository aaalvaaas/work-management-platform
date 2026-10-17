package ru.itmo.wmp.platform.dto.mailbox;

public record InternalMailboxResponse(
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
