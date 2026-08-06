package ru.itmo.wmp.mail.dto.response;

import ru.itmo.wmp.mail.domain.MailProcessingStatus;

import java.time.LocalDateTime;

public record MailResponse(
    String messageId,
    String senderEmail,
    String recipientEmail,
    String subject,
    String plainTextBody,
    MailProcessingStatus processingStatus,
    LocalDateTime receivedAt,
    LocalDateTime processedAt
) {
}
