package ru.itmo.wmp.mail.application.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.itmo.wmp.mail.application.MailProcessor;
import ru.itmo.wmp.mail.application.MailService;
import ru.itmo.wmp.mail.application.archive.ArchivedAttachment;
import ru.itmo.wmp.mail.application.archive.ArchivedMail;
import ru.itmo.wmp.mail.application.archive.MailArchive;
import ru.itmo.wmp.mail.application.attachment.MailAttachmentService;
import ru.itmo.wmp.mail.application.mailbox.MailboxConnection;
import ru.itmo.wmp.mail.application.mailbox.MailboxDirectory;
import ru.itmo.wmp.mail.domain.MailAttachment;
import ru.itmo.wmp.mail.domain.MailMessage;
import ru.itmo.wmp.mail.domain.MailProcessingStatus;
import ru.itmo.wmp.mail.exception.DuplicateMailException;
import ru.itmo.wmp.mail.infrastructure.imap.ImapMailClient;
import ru.itmo.wmp.mail.infrastructure.imap.ParsedMail;
import ru.itmo.wmp.mail.infrastructure.imap.RawMailContent;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailImportService {
    private final ImapMailClient imapMailClient;
    private final MailService mailService;
    private final MailProcessor mailProcessor;
    private final MailAttachmentService mailAttachmentService;
    private final MailboxDirectory mailboxDirectory;
    private final MailArchive mailArchive;

    public MailImportResult importUnreadMails() {
        AtomicInteger imported = new AtomicInteger();
        AtomicInteger skipped = new AtomicInteger();

        for (MailboxConnection mailbox : mailboxDirectory.activeMailboxes()) {
            importFrom(mailbox, imported, skipped);
        }

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

    private void importFrom(
        MailboxConnection mailbox,
        AtomicInteger imported,
        AtomicInteger skipped
    ) {
        List<ParsedMail> mails;

        try {
            mails = imapMailClient.fetchUnread(mailbox);

            mailboxDirectory.reportCheck(mailbox.id(), true, null);
        } catch (RuntimeException exception) {
            log.warn(
                "Mailbox {} of organization {} could not be read: {}",
                mailbox.username(),
                mailbox.organizationId(),
                exception.getMessage()
            );

            mailboxDirectory.reportCheck(mailbox.id(), false, exception.getMessage());

            return;
        }

        mails.forEach(parsedMail -> {
            try {
                parsedMail.mailMessage().setOrganizationId(mailbox.organizationId());
                parsedMail.mailMessage().setMailboxId(mailbox.id());

                MailMessage savedMail = mailService.receiveMail(parsedMail.mailMessage());

                List<MailAttachment> attachments = parsedMail.attachments()
                    .stream()
                    .map(attachment -> mailAttachmentService.save(savedMail, attachment))
                    .toList();

                archive(savedMail, parsedMail.rawContent(), attachments);

                mailProcessor.process(savedMail);
                imapMailClient.markAsRead(parsedMail.sourceMessage());

                log.info(
                    "Imported mail: messageId={}, subject='{}'",
                    parsedMail.mailMessage().getMessageId(),
                    parsedMail.mailMessage().getSubject()
                );

                imported.getAndIncrement();
            } catch (DuplicateMailException duplicate) {
                retryOrSkip(parsedMail, imported, skipped);
            } catch (RuntimeException failure) {
                log.warn(
                    "Mail {} could not be processed: {}",
                    parsedMail.mailMessage().getMessageId(),
                    failure.getMessage()
                );
            }
        });
    }

    private void archive(
        MailMessage mail,
        RawMailContent rawContent,
        List<MailAttachment> attachments
    ) {
        try {
            mailArchive.store(new ArchivedMail(
                mail.getMessageId(),
                mail.getOrganizationId(),
                mail.getMailboxId(),
                mail.getSenderEmail(),
                mail.getRecipientEmail(),
                mail.getSubject(),
                mail.getReceivedAt(),
                rawContent.headers(),
                rawContent.textBody(),
                rawContent.htmlBody(),
                attachments.stream()
                    .map(attachment -> new ArchivedAttachment(
                        attachment.getFilename(),
                        attachment.getContentType(),
                        attachment.getSize(),
                        attachment.getStorageKey()
                    ))
                    .toList()
            ));
        } catch (RuntimeException exception) {
            log.warn(
                "Mail {} is not archived: {}",
                mail.getMessageId(),
                exception.getMessage()
            );
        }
    }

    private void retryOrSkip(
        ParsedMail parsedMail,
        AtomicInteger imported,
        AtomicInteger skipped
    ) {
        String messageId = parsedMail.mailMessage().getMessageId();
        MailMessage stored = mailService.getMail(messageId);

        if (stored.getProcessingStatus() != MailProcessingStatus.FAILED) {
            log.debug("Skipped duplicate mail: messageId={}", messageId);

            skipped.getAndIncrement();

            return;
        }

        try {
            mailProcessor.process(stored);
            imapMailClient.markAsRead(parsedMail.sourceMessage());

            log.info("Processed mail {} after an earlier failure", messageId);

            imported.getAndIncrement();
        } catch (RuntimeException failure) {
            log.warn("Mail {} failed again: {}", messageId, failure.getMessage());
        }
    }
}
