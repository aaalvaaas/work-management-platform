package ru.itmo.wmp.mail.infrastructure.imap;

import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeUtility;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class MailAttachmentExtractor {
    public List<ParsedAttachment> extract(Part part) throws MessagingException, IOException {
        List<ParsedAttachment> attachments = new ArrayList<>();

        extractAttachments(part, attachments);

        return attachments;
    }

    private void extractAttachments(Part part, List<ParsedAttachment> attachments) throws MessagingException, IOException {
        if (!part.isMimeType("multipart/*")) return;

        Multipart multipart = (Multipart) part.getContent();

        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);

            if (isAttachment(bodyPart)) {
                attachments.add(createAttachment(bodyPart));
                continue;
            }

            if (bodyPart.isMimeType("multipart/*")) extractAttachments(bodyPart, attachments);
        }
    }

    private boolean isAttachment(Part part) throws MessagingException {
        String disposition = part.getDisposition();
        String filename = part.getFileName();

        return Part.ATTACHMENT.equalsIgnoreCase(disposition) || filename != null;
    }

    private ParsedAttachment createAttachment(Part part) throws MessagingException, IOException {
        String filename = MimeUtility.decodeText(part.getFileName());
        String contentType = part.getContentType();

        int separator = contentType.indexOf(';');

        if (separator >= 0) {
            contentType = contentType.substring(0, separator);
        }

        contentType = contentType.trim();

        byte[] content = part.getInputStream().readAllBytes();

        return new ParsedAttachment(
            filename,
            contentType,
            content.length,
            new ByteArrayInputStream(content)
        );
    }
}
