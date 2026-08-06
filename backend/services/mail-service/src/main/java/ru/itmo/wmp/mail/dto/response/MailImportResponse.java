package ru.itmo.wmp.mail.dto.response;

public record MailImportResponse(
    int imported,
    int skipped
) {
}
