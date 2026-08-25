package ru.itmo.wmp.mail.infrastructure.imap;

import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import org.springframework.stereotype.Component;

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
        return Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition());
    }

    private ParsedAttachment createAttachment(Part part) throws MessagingException, IOException {
        String filename = part.getFileName();
        String contentType = part.getContentType();

        return new ParsedAttachment(
            filename,
            contentType,
            part.getSize(),
            part.getInputStream()
        );
    }
}
