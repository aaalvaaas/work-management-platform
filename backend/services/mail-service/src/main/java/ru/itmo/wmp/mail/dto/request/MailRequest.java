package ru.itmo.wmp.mail.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record MailRequest(
    @NotBlank(message = "MessageId is required")
    String messageId,

    @NotBlank(message = "SenderEmail is required")
    @Email(message = "Invalid senderEmail")
    String senderEmail,

    @NotBlank(message = "RecipientEmail is required")
    @Email(message = "Invalid recipientEmail")
    String recipientEmail,

    @NotBlank(message = "Subject is required")
    String subject,

    @NotBlank(message = "PlainTextBody is required")
    String plainTextBody
) {
}
