package ru.itmo.wmp.mail.imap;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.wmp.mail.domain.MailMessage;
import ru.itmo.wmp.mail.infrastructure.imap.MailContentExtractor;
import ru.itmo.wmp.mail.infrastructure.imap.MailParser;

import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MailParserTest {
    @Mock
    private MailContentExtractor contentExtractor;

    @InjectMocks
    private MailParser mailParser;

    @Test
    void shouldParseMailSuccessfully() throws MessagingException, IOException {
        MimeMessage message = createMessage();

        when(contentExtractor.extractText(message))
            .thenReturn("Test Body");

        MailMessage result = mailParser.parse(message);

        assertEquals("<test-001>@test.ru", result.getMessageId());
        assertEquals("sender@test.ru", result.getSenderEmail());
        assertEquals("recipient@test.ru", result.getRecipientEmail());
        assertEquals("Test Subject", result.getSubject());
        assertEquals("Test Body", result.getPlainTextBody());

        verify(contentExtractor)
            .extractText(message);
    }

    @Test
    void shouldThrowExceptionWhenMessageIdMissing() throws MessagingException {
        Session session = Session.getDefaultInstance(new Properties());

        MimeMessage message = new MimeMessage(session);

        message.setFrom("sender@test.ru");
        message.setRecipients(Message.RecipientType.TO, "recipient@test.ru");
        message.setSubject("Test subject");

        assertThrows(
            IllegalStateException.class,
            () -> mailParser.parse(message)
        );

        verifyNoInteractions(contentExtractor);
    }

    @Test
    void shouldDecodeMimeSubject() throws MessagingException, IOException {
        MimeMessage message = createMessage();

        message.setSubject("=?UTF-8?B?0J/RgNC40LLQtdGC?=");

        when(contentExtractor.extractText(message))
            .thenReturn("Test Body");

        MailMessage result = mailParser.parse(message);

        assertEquals("Привет", result.getSubject());

        verify(contentExtractor)
            .extractText(message);
    }

    private MimeMessage createMessage() throws MessagingException {
        Session session = Session.getDefaultInstance(new Properties());

        MimeMessage message = new MimeMessage(session);

        message.setHeader("Message-ID", "<test-001>@test.ru");
        message.setFrom("sender@test.ru");
        message.setRecipients(Message.RecipientType.TO, "recipient@test.ru");
        message.setSubject("Test Subject");

        return message;
    }
}
