package ru.itmo.wmp.mail.infrastructure.mongo;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.itmo.wmp.mail.application.archive.ArchivedAttachment;
import ru.itmo.wmp.mail.application.archive.ArchivedMail;
import ru.itmo.wmp.mail.application.archive.MailArchive;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MongoMailArchive implements MailArchive {
    private final MongoMailDocumentRepository repository;

    @Override
    public void store(ArchivedMail mail) {
        repository.save(toDocument(mail));
    }

    @Override
    public Optional<ArchivedMail> find(String messageId) {
        return repository.findById(messageId).map(this::toArchivedMail);
    }

    private MailDocument toDocument(ArchivedMail mail) {
        MailDocument document = new MailDocument();

        document.setMessageId(mail.messageId());
        document.setOrganizationId(mail.organizationId());
        document.setMailboxId(mail.mailboxId());
        document.setSenderEmail(mail.senderEmail());
        document.setRecipientEmail(mail.recipientEmail());
        document.setSubject(mail.subject());
        document.setReceivedAt(mail.receivedAt());
        document.setHeaders(mail.headers());
        document.setTextBody(mail.textBody());
        document.setHtmlBody(mail.htmlBody());
        document.setAttachments(mail.attachments().stream().map(this::toAttachment).toList());
        document.setStoredAt(LocalDateTime.now());

        return document;
    }

    private MailDocument.Attachment toAttachment(ArchivedAttachment attachment) {
        MailDocument.Attachment stored = new MailDocument.Attachment();

        stored.setFilename(attachment.filename());
        stored.setContentType(attachment.contentType());
        stored.setSize(attachment.size());
        stored.setStorageKey(attachment.storageKey());

        return stored;
    }

    private ArchivedMail toArchivedMail(MailDocument document) {
        List<ArchivedAttachment> attachments = document.getAttachments() == null
            ? List.of()
            : document.getAttachments()
                .stream()
                .map(attachment -> new ArchivedAttachment(
                    attachment.getFilename(),
                    attachment.getContentType(),
                    attachment.getSize(),
                    attachment.getStorageKey()
                ))
                .toList();

        return new ArchivedMail(
            document.getMessageId(),
            document.getOrganizationId(),
            document.getMailboxId(),
            document.getSenderEmail(),
            document.getRecipientEmail(),
            document.getSubject(),
            document.getReceivedAt(),
            document.getHeaders() == null ? Map.of() : document.getHeaders(),
            document.getTextBody(),
            document.getHtmlBody(),
            attachments
        );
    }
}
