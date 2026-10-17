package ru.itmo.wmp.platform.dto.mailbox;

import ru.itmo.wmp.platform.domain.mailbox.MailboxStatus;

import java.time.LocalDateTime;

public record MailboxResponse(
    Long id,
    Long organizationId,
    String host,
    Integer port,
    String username,
    String folder,
    boolean useSsl,
    MailboxStatus status,
    String lastError,
    LocalDateTime lastCheckedAt
) {
}
