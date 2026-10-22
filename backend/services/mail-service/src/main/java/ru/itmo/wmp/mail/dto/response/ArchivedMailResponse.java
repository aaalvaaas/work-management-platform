package ru.itmo.wmp.mail.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ArchivedMailResponse(
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
    List<ArchivedAttachmentResponse> attachments
) {
    public record ArchivedAttachmentResponse(
        String filename,
        String contentType,
        long size,
        String storageKey
    ) {
    }
}
