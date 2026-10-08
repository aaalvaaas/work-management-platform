package ru.itmo.wmp.mail.application.mail;

public record MailImportResult(
    int imported,
    int skipped
) {
}
