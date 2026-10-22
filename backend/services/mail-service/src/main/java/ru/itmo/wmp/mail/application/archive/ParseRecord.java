package ru.itmo.wmp.mail.application.archive;

import java.time.LocalDateTime;
import java.util.Map;

public record ParseRecord(
    String messageId,
    Long organizationId,
    String parser,
    String parserVersion,
    LocalDateTime parsedAt,
    String title,
    String description,
    boolean requiresApproval,
    String requesterEmail,
    Double confidence,
    Map<String, Object> details
) {
}
