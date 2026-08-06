package ru.itmo.wmp.mail.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.wmp.mail.domain.MailMessage;

@Service
@RequiredArgsConstructor
public class MailProcessor {
    private final MailService mailService;

    public void process(MailMessage mail) {
        String messageId = mail.getMessageId();

        mailService.markAsProcessing(messageId);

        try {
            mailService.markAsProcessed(messageId);
        } catch (Exception e) {
            mailService.markAsFailed(messageId);
            throw e;
        }
    }
}
