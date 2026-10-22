package ru.itmo.wmp.mail.application.archive;

public record ArchivedAttachment(
    String filename,
    String contentType,
    long size,
    String storageKey
) {
}
