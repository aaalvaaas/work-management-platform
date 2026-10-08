package ru.itmo.wmp.mail.dto.response;

public record MailAttachmentResponse(
    Long id,
    String filename,
    String contentType,
    Long size
) {
}
