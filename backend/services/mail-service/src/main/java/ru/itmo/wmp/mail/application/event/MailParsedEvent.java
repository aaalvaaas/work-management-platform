package ru.itmo.wmp.mail.application.event;

import java.time.LocalDateTime;

public record MailParsedEvent(
    String messageId,
    Long organizationId,
    Long mailboxId,
    String requesterEmail,
    String subject,
    LocalDateTime receivedAt,
    String title,
    String description,
    boolean requiresApproval
) {
}
