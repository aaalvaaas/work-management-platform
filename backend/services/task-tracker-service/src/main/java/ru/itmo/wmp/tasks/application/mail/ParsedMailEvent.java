package ru.itmo.wmp.tasks.application.mail;

import java.time.LocalDateTime;

public record ParsedMailEvent(
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
