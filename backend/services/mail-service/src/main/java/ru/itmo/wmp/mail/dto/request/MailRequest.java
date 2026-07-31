package ru.itmo.wmp.mail.dto.request;

public record MailRequest(
    String messageId,
    String senderEmail,
    String recipientEmail,
    String subject,
    String plainTextBody
) {
}
