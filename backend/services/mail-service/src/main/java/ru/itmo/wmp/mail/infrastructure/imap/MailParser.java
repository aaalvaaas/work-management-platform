package ru.itmo.wmp.mail.infrastructure.imap;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.itmo.wmp.mail.domain.MailMessage;

import jakarta.mail.Header;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MailParser {
    private final MailContentExtractor contentExtractor;
    private final MailAttachmentExtractor attachmentExtractor;

    public ParsedMail parse(Message message) throws MessagingException, IOException {
        MailMessage mail = new MailMessage();

        mail.setMessageId(getMessageId(message));
        mail.setSenderEmail(getSenderEmail(message));
        mail.setRecipientEmail(getRecipientEmail(message));
        mail.setSubject(getSubject(message));

        RawMailContent rawContent = new RawMailContent(
            headersOf(message),
            getBody(message),
            getHtmlBody(message)
        );

        mail.setPlainTextBody(rawContent.textBody());

        List<ParsedAttachment> attachments = attachmentExtractor.extract(message);

        return new ParsedMail(
            message,
            mail,
            attachments,
            rawContent
        );
    }

    private Map<String, List<String>> headersOf(Message message) throws MessagingException {
        Map<String, List<String>> headers = new LinkedHashMap<>();

        Enumeration<Header> all = message.getAllHeaders();

        while (all.hasMoreElements()) {
            Header header = all.nextElement();

            headers
                .computeIfAbsent(header.getName().toLowerCase(), key -> new ArrayList<>())
                .add(decodeMimeText(header.getValue()));
        }

        return headers;
    }

    private String getHtmlBody(Message message) throws MessagingException {
        try {
            return contentExtractor.extractHtml(message);
        } catch (Exception e) {
            return "";
        }
    }

    private String getMessageId(Message message) throws MessagingException {
        String[] headers = message.getHeader("Message-ID");

        if (headers == null || headers.length == 0) throw new IllegalStateException("Message-ID header is missing");

        return headers[0];
    }

    private String getSenderEmail(Message message) throws MessagingException {
        Address[] senders = message.getFrom();

        if (senders == null || senders.length == 0) return null;

        return ((InternetAddress) senders[0])
            .getAddress();
    }

    private String getRecipientEmail(Message message) throws MessagingException {
        Address[] recipients = message.getRecipients(Message.RecipientType.TO);

        if (recipients == null || recipients.length == 0) throw new IllegalStateException("Recipient is missing");

        return ((InternetAddress) recipients[0])
            .getAddress();
    }

    private String getSubject(Message message) throws MessagingException {
        return decodeMimeText(message.getSubject());
    }

    private String getBody(Message message) throws MessagingException {
        try {
            return contentExtractor.extractText(message);
        } catch (Exception e) {
            throw new MessagingException("Cannot parse mail body", e);
        }
    }

    private String decodeMimeText(String value) {
        if (value == null) return "";

        try {
            return MimeUtility.decodeText(value);
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }
}
