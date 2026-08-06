package ru.itmo.wmp.mail.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.itmo.wmp.mail.exception.DuplicateMailException;
import ru.itmo.wmp.mail.infrastructure.imap.ImapMailClient;
import ru.itmo.wmp.mail.infrastructure.imap.ParsedMail;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
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

                log.info(
                    "Imported mail: messageId={}, subject='{}'",
                    parsedMail.mailMessage().getMessageId(),
                    parsedMail.mailMessage().getSubject()
                );

                imported.getAndIncrement();
            } catch (DuplicateMailException ignored) {
                log.debug(
                    "Skipped duplicate mail: messageId={}",
                    parsedMail.mailMessage().getMessageId()
                );

                skipped.getAndIncrement();
            }
        });

        log.info(
            "Mail import completed: imported={}, skipped={}",
            imported.get(),
            skipped.get()
        );

        return new MailImportResult(
            imported.get(),
            skipped.get()
        );
    }
}
