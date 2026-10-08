package ru.itmo.wmp.mail.infrastructure.imap;

import java.io.InputStream;

public record ParsedAttachment(
    String filename,
    String contentType,
    long size,
    InputStream inputStream
) {
}
