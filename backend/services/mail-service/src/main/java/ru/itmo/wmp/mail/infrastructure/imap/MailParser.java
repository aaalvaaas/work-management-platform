package ru.itmo.wmp.mail.infrastructure.imap;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeUtility;
import org.springframework.stereotype.Component;
import ru.itmo.wmp.mail.domain.MailMessage;

import java.io.UnsupportedEncodingException;

@Component
public class MailParser {
    public MailMessage parse(Message message) throws MessagingException {
        MailMessage mail = new MailMessage();

        mail.setMessageId(getMessageId(message));
        mail.setSenderEmail(getSenderEmail(message));
        mail.setRecipientEmail(getRecipientEmail(message));
        mail.setSubject(getSubject(message));
        mail.setPlainTextBody(getBody(message));

        return mail;
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
            Object content = message.getContent();

            if (content instanceof String text) return text;

            return content.toString();
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
