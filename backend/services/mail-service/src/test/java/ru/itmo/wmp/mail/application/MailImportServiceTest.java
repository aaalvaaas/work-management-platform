package ru.itmo.wmp.mail.application;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.wmp.mail.application.archive.ArchivedMail;
import ru.itmo.wmp.mail.application.archive.MailArchive;
import ru.itmo.wmp.mail.application.attachment.MailAttachmentService;
import ru.itmo.wmp.mail.application.mailbox.MailboxConnection;
import ru.itmo.wmp.mail.application.mailbox.MailboxDirectory;
import ru.itmo.wmp.mail.application.mail.MailImportResult;
import ru.itmo.wmp.mail.application.mail.MailImportService;
import ru.itmo.wmp.mail.domain.MailMessage;
import ru.itmo.wmp.mail.domain.MailProcessingStatus;
import ru.itmo.wmp.mail.exception.DuplicateMailException;
import ru.itmo.wmp.mail.exception.MailImportException;
import ru.itmo.wmp.mail.infrastructure.imap.ImapMailClient;
import ru.itmo.wmp.mail.infrastructure.imap.ParsedAttachment;
import ru.itmo.wmp.mail.infrastructure.imap.ParsedMail;

import java.util.List;

import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MailImportServiceTest {
    private static final MailboxConnection MAILBOX = new MailboxConnection(
        1L,
        10L,
        "megafaculty",
        "imap.mail.ru",
        993,
        "megafaculty@mail.ru",
        "secret",
        "INBOX",
        true
    );

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

    @Mock
    private MailboxDirectory mailboxDirectory;

    @Mock
    private MailArchive mailArchive;

    @InjectMocks
    private MailImportService mailImportService;

    @BeforeEach
    void stubMailboxes() {
        when(mailboxDirectory.activeMailboxes()).thenReturn(List.of(MAILBOX));
    }

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

        when(imapMailClient.fetchUnread(MAILBOX))
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
    void shouldArchiveEveryImportedMail() {
        MailMessage mail = new MailMessage();
        mail.setMessageId("test-001");
        mail.setSubject("СОГЛАСОВАНИЕ: закупка");

        when(imapMailClient.fetchUnread(MAILBOX))
            .thenReturn(List.of(new ParsedMail(sourceMessage1, mail, List.of())));

        when(mailService.receiveMail(mail))
            .thenReturn(mail);

        mailImportService.importUnreadMails();

        ArgumentCaptor<ArchivedMail> captor = ArgumentCaptor.forClass(ArchivedMail.class);

        verify(mailArchive).store(captor.capture());

        assertEquals("test-001", captor.getValue().messageId());
        assertEquals("СОГЛАСОВАНИЕ: закупка", captor.getValue().subject());
    }

    @Test
    void shouldImportMailEvenWhenArchivingFails() {
        MailMessage mail = new MailMessage();
        mail.setMessageId("test-001");

        when(imapMailClient.fetchUnread(MAILBOX))
            .thenReturn(List.of(new ParsedMail(sourceMessage1, mail, List.of())));

        when(mailService.receiveMail(mail))
            .thenReturn(mail);

        doThrow(new RuntimeException("Mongo is down"))
            .when(mailArchive)
            .store(any());

        MailImportResult result = mailImportService.importUnreadMails();

        assertEquals(1, result.imported());

        verify(mailProcessor).process(mail);

        verify(imapMailClient).markAsRead(sourceMessage1);
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

        when(imapMailClient.fetchUnread(MAILBOX))
            .thenReturn(List.of(
                duplicateParsed,
                normalParsed
            ));

        doThrow(new DuplicateMailException("test-001"))
            .when(mailService)
            .receiveMail(duplicateMail);

        when(mailService.receiveMail(normalMail))
            .thenReturn(normalMail);

        MailMessage stored = new MailMessage();
        stored.setMessageId("test-001");
        stored.setProcessingStatus(MailProcessingStatus.PROCESSED);

        when(mailService.getMail("test-001"))
            .thenReturn(stored);

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
        when(imapMailClient.fetchUnread(MAILBOX))
            .thenReturn(List.of());

        MailImportResult result = mailImportService.importUnreadMails();

        assertEquals(0, result.imported());
        assertEquals(0, result.skipped());

        verify(imapMailClient)
            .fetchUnread(MAILBOX);

        verify(mailService, never())
            .receiveMail(any());

        verify(imapMailClient, never())
            .markAsRead(any());
    }

    @Test
    void shouldSkipMailboxThatCannotBeRead() {
        when(imapMailClient.fetchUnread(MAILBOX))
            .thenThrow(new MailImportException("IMAP failed", new MessagingException()));

        MailImportResult result = mailImportService.importUnreadMails();

        assertEquals(0, result.imported());
        assertEquals(0, result.skipped());

        verify(mailService, never())
            .receiveMail(any());

        verify(imapMailClient, never())
            .markAsRead(any());
    }

    @Test
    void shouldProcessAgainAMailThatFailedEarlier() {
        MailMessage mail = new MailMessage();
        mail.setMessageId("test-001");

        MailMessage stored = new MailMessage();
        stored.setMessageId("test-001");
        stored.setProcessingStatus(MailProcessingStatus.FAILED);

        when(imapMailClient.fetchUnread(MAILBOX))
            .thenReturn(List.of(new ParsedMail(sourceMessage1, mail, List.of())));

        doThrow(new DuplicateMailException("test-001"))
            .when(mailService)
            .receiveMail(mail);

        when(mailService.getMail("test-001"))
            .thenReturn(stored);

        MailImportResult result = mailImportService.importUnreadMails();

        assertEquals(1, result.imported());
        assertEquals(0, result.skipped());

        verify(mailProcessor)
            .process(stored);

        verify(imapMailClient)
            .markAsRead(sourceMessage1);
    }

    @Test
    void shouldKeepImportingWhenOneMailCannotBeProcessed() {
        MailMessage failing = new MailMessage();
        failing.setMessageId("test-001");

        MailMessage normal = new MailMessage();
        normal.setMessageId("test-002");

        when(imapMailClient.fetchUnread(MAILBOX))
            .thenReturn(List.of(
                new ParsedMail(sourceMessage1, failing, List.of()),
                new ParsedMail(sourceMessage2, normal, List.of())
            ));

        when(mailService.receiveMail(failing))
            .thenReturn(failing);

        when(mailService.receiveMail(normal))
            .thenReturn(normal);

        doThrow(new RuntimeException("Broker is down"))
            .when(mailProcessor)
            .process(failing);

        MailImportResult result = mailImportService.importUnreadMails();

        assertEquals(1, result.imported());

        verify(imapMailClient, never())
            .markAsRead(sourceMessage1);

        verify(imapMailClient)
            .markAsRead(sourceMessage2);
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

        when(imapMailClient.fetchUnread(MAILBOX))
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
