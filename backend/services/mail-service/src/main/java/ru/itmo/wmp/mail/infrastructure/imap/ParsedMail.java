package ru.itmo.wmp.mail.infrastructure.imap;

import jakarta.mail.Message;
import ru.itmo.wmp.mail.domain.MailMessage;

import java.util.List;

public record ParsedMail(
    Message sourceMessage,
    MailMessage mailMessage,
    List<ParsedAttachment> attachments
) {
}
