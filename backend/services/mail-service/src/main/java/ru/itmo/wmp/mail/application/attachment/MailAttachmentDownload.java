package ru.itmo.wmp.mail.application.attachment;

import java.io.InputStream;

public record MailAttachmentDownload(
    String filename,
    String contentType,
    Long size,
    InputStream inputStream
) {
}
