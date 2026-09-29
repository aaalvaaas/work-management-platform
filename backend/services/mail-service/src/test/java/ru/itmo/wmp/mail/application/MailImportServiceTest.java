package ru.itmo.wmp.mail.application;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.wmp.mail.domain.MailMessage;
import ru.itmo.wmp.mail.exception.DuplicateMailException;
import ru.itmo.wmp.mail.exception.MailImportException;
import ru.itmo.wmp.mail.infrastructure.imap.ImapMailClient;
import ru.itmo.wmp.mail.infrastructure.imap.ParsedAttachment;
import ru.itmo.wmp.mail.infrastructure.imap.ParsedMail;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MailImportServiceTest {
    @Mock
    private ImapMailClient imapMailClient;

    @Mock
    private MailService mailService;

    @Mock
    private Message sourceMessage1;

    @Mock
    private Message sourceMessage2;

    @Mock
    private MailProcessor mailProcessor;

    @Mock
    private MailAttachmentService mailAttachmentService;

    @InjectMocks
    private MailImportService mailImportService;

    @Test
    void shouldImportUnreadMailsSuccessfully() {
        MailMessage mail1 = new MailMessage();
        mail1.setMessageId("test-001");

        MailMessage mail2 = new MailMessage();
        mail2.setMessageId("test-002");

        ParsedMail parsedMail1 = new ParsedMail(
            sourceMessage1,
            mail1,
            List.of()
        );

        ParsedMail parsedMail2 = new ParsedMail(
            sourceMessage2,
            mail2,
            List.of()
        );

        when(imapMailClient.fetchUnread())
            .thenReturn(List.of(
                parsedMail1,
                parsedMail2
            ));

        when(mailService.receiveMail(mail1))
            .thenReturn(mail1);

        when(mailService.receiveMail(mail2))
            .thenReturn(mail2);

        MailImportResult result =
            mailImportService.importUnreadMails();

        assertEquals(2, result.imported());
        assertEquals(0, result.skipped());

        verify(mailService)
            .receiveMail(mail1);

        verify(mailService)
            .receiveMail(mail2);

        verify(imapMailClient)
            .markAsRead(sourceMessage1);

        verify(imapMailClient)
            .markAsRead(sourceMessage2);

        verify(mailProcessor)
            .process(mail1);

        verify(mailProcessor)
            .process(mail2);

        verify(mailAttachmentService, never())
            .save(any(MailMessage.class), any(ParsedAttachment.class));
    }

    @Test
    void shouldSkipDuplicateMails() {
        MailMessage duplicateMail = new MailMessage();
        duplicateMail.setMessageId("test-001");

        MailMessage normalMail = new MailMessage();
        normalMail.setMessageId("test-002");

        ParsedMail duplicateParsed =
            new ParsedMail(
                sourceMessage1,
                duplicateMail,
                List.of()
            );

        ParsedMail normalParsed =
            new ParsedMail(
                sourceMessage2,
                normalMail,
                List.of()
            );

        when(imapMailClient.fetchUnread())
            .thenReturn(List.of(
                duplicateParsed,
                normalParsed
            ));

        doThrow(new DuplicateMailException("test-001"))
            .when(mailService)
            .receiveMail(duplicateMail);

        MailImportResult result =
            mailImportService.importUnreadMails();

        assertEquals(1, result.imported());
        assertEquals(1, result.skipped());

        verify(mailService)
            .receiveMail(duplicateMail);

        verify(mailService)
            .receiveMail(normalMail);

        verify(imapMailClient, never())
            .markAsRead(sourceMessage1);

        verify(imapMailClient)
            .markAsRead(sourceMessage2);

        verify(mailAttachmentService, never())
            .save(any(MailMessage.class), any(ParsedAttachment.class));
    }

    @Test
    void shouldReturnEmptyResultWhenNoUnreadMails() {
        when(imapMailClient.fetchUnread())
            .thenReturn(List.of());

        MailImportResult result = mailImportService.importUnreadMails();

        assertEquals(0, result.imported());
        assertEquals(0, result.skipped());

        verify(imapMailClient)
            .fetchUnread();

        verify(mailService, never())
            .receiveMail(any());

        verify(imapMailClient, never())
            .markAsRead(any());
    }

    @Test
    void shouldThrowExceptionWhenImapFails() {
        when(imapMailClient.fetchUnread())
            .thenThrow(new MailImportException("IMAP failed", new MessagingException()));

        assertThrows(
            MailImportException.class,
            () -> mailImportService.importUnreadMails()
        );

        verify(mailService, never())
            .receiveMail(any());

        verify(imapMailClient, never())
            .markAsRead(any());
    }

    @Test
    void shouldImportMailAttachments() {
        MailMessage mail = new MailMessage();
        mail.setMessageId("test-001");

        ParsedAttachment attachment1 = mock(ParsedAttachment.class);
        ParsedAttachment attachment2 = mock(ParsedAttachment.class);

        ParsedMail parsedMail = new ParsedMail(
            sourceMessage1,
            mail,
            List.of(attachment1, attachment2)
        );

        when(imapMailClient.fetchUnread())
            .thenReturn(List.of(parsedMail));

        when(mailService.receiveMail(mail))
            .thenReturn(mail);

        MailImportResult result = mailImportService.importUnreadMails();

        assertEquals(1, result.imported());
        assertEquals(0, result.skipped());

        verify(mailAttachmentService)
            .save(mail, attachment1);

        verify(mailAttachmentService)
            .save(mail, attachment2);

        verify(mailProcessor)
            .process(mail);

        verify(imapMailClient)
            .markAsRead(sourceMessage1);
    }
}
