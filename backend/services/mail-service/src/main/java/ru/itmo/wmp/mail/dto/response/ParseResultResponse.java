package ru.itmo.wmp.mail.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

public record ParseResultResponse(
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
