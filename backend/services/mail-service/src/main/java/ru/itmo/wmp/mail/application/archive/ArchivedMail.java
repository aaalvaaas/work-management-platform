package ru.itmo.wmp.mail.application.archive;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ArchivedMail(
    String messageId,
    Long organizationId,
    Long mailboxId,
    String senderEmail,
    String recipientEmail,
    String subject,
    LocalDateTime receivedAt,
    Map<String, List<String>> headers,
    String textBody,
    String htmlBody,
    List<ArchivedAttachment> attachments
) {
}
