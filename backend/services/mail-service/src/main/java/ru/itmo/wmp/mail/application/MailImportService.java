package ru.itmo.wmp.mail.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.wmp.mail.exception.DuplicateMailException;
import ru.itmo.wmp.mail.infrastructure.imap.ImapMailClient;
import ru.itmo.wmp.mail.infrastructure.imap.ParsedMail;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class MailImportService {
    private final ImapMailClient imapMailClient;
    private final MailService mailService;

    public MailImportResult importUnreadMails() {
        List<ParsedMail> mails = imapMailClient.fetchUnread();

        AtomicInteger imported = new AtomicInteger();
        AtomicInteger skipped = new AtomicInteger();

        mails.forEach(parsedMail -> {
            try {
                mailService.receiveMail(parsedMail.mailMessage());
                imapMailClient.markAsRead(parsedMail.sourceMessage());
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
