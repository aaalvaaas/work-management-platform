package ru.itmo.wmp.mail.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.wmp.mail.domain.MailMessage;
import ru.itmo.wmp.mail.dto.response.MailImportResponse;
import ru.itmo.wmp.mail.exception.DuplicateMailException;
import ru.itmo.wmp.mail.infrastructure.imap.ImapMailClient;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class MailImportService {
    private final ImapMailClient imapMailClient;
    private final MailService mailService;

    public MailImportResult importUnreadMails() {
        List<MailMessage> mails = imapMailClient.fetchUnread();

        AtomicInteger imported = new AtomicInteger();
        AtomicInteger skipped = new AtomicInteger();

        mails.forEach(mail -> {
            try {
                mailService.receiveMail(mail);
                imported.getAndIncrement();
            } catch (DuplicateMailException ignored) {
                skipped.getAndIncrement();
            }
        });

        return new MailImportResult(
            imported.get(),
            skipped.get()
        );
    }
}
