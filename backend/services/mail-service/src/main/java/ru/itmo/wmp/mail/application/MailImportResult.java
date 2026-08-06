package ru.itmo.wmp.mail.application;

public record MailImportResult(
    int imported,
    int skipped
) {
}
