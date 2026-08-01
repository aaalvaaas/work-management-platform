package ru.itmo.wmp.mail.infrastructure.imap;

import jakarta.mail.Message;
import ru.itmo.wmp.mail.domain.MailMessage;

public record ParsedMail(
    Message sourceMessage,
    MailMessage mailMessage
) {
}
