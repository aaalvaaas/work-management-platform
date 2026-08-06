package ru.itmo.wmp.mail.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.wmp.mail.domain.MailMessage;

@Service
@RequiredArgsConstructor
public class MailProcessor {
    private final MailService mailService;

    public void process(MailMessage mail) {
        mailService.markAsProcessing(mail.getMessageId());

        mailService.markAsProcessed(mail.getMessageId());
    }
}
