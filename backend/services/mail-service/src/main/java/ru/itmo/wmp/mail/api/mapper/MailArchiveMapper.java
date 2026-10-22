package ru.itmo.wmp.mail.api.mapper;

import org.springframework.stereotype.Component;
import ru.itmo.wmp.mail.application.archive.ArchivedMail;
import ru.itmo.wmp.mail.application.archive.ParseRecord;
import ru.itmo.wmp.mail.dto.response.ArchivedMailResponse;
import ru.itmo.wmp.mail.dto.response.ParseResultResponse;

@Component
public class MailArchiveMapper {
    public ArchivedMailResponse toResponse(ArchivedMail mail) {
        return new ArchivedMailResponse(
            mail.messageId(),
            mail.organizationId(),
            mail.mailboxId(),
            mail.senderEmail(),
            mail.recipientEmail(),
            mail.subject(),
            mail.receivedAt(),
            mail.headers(),
            mail.textBody(),
            mail.htmlBody(),
            mail.attachments()
                .stream()
                .map(attachment -> new ArchivedMailResponse.ArchivedAttachmentResponse(
                    attachment.filename(),
                    attachment.contentType(),
                    attachment.size(),
                    attachment.storageKey()
                ))
                .toList()
        );
    }

    public ParseResultResponse toResponse(ParseRecord parseRecord) {
        return new ParseResultResponse(
            parseRecord.parser(),
            parseRecord.parserVersion(),
            parseRecord.parsedAt(),
            parseRecord.title(),
            parseRecord.description(),
            parseRecord.requiresApproval(),
            parseRecord.requesterEmail(),
            parseRecord.confidence(),
            parseRecord.details()
        );
    }
}
