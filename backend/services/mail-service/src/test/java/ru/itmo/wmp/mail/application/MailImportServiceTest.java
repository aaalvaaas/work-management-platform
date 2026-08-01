package ru.itmo.wmp.mail.application;

import jakarta.mail.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.wmp.mail.domain.MailMessage;
import ru.itmo.wmp.mail.exception.DuplicateMailException;
import ru.itmo.wmp.mail.infrastructure.imap.ImapMailClient;
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

    private MailImportService mailImportService;

    @Test
    void shouldImportUnreadMailsSuccessfully() {
        mailImportService = new MailImportService(
            imapMailClient,
            mailService
        );

        MailMessage mail1 = new MailMessage();
        mail1.setMessageId("test-001");

        MailMessage mail2 = new MailMessage();
        mail2.setMessageId("test-002");

        ParsedMail parsedMail1 = new ParsedMail(
            sourceMessage1,
            mail1
        );

        ParsedMail parsedMail2 = new ParsedMail(
            sourceMessage2,
            mail2
        );

        when(imapMailClient.fetchUnread())
            .thenReturn(List.of(
                parsedMail1,
                parsedMail2
            ));

        MailImportResult result =
            mailImportService.importUnreadMails();

        assertEquals(2, result.imported());
        assertEquals(0, result.skipped());

        verify(mailService)
            .receiveMail(mail1);

        verify(mailService)
            .receiveMail(mail2);
    }

    @Test
    void shouldSkipDuplicateMails() {
        mailImportService = new MailImportService(
            imapMailClient,
            mailService
        );

        MailMessage duplicateMail = new MailMessage();
        duplicateMail.setMessageId("test-001");

        MailMessage normalMail = new MailMessage();
        normalMail.setMessageId("test-002");

        ParsedMail duplicateParsed =
            new ParsedMail(
                sourceMessage1,
                duplicateMail
            );

        ParsedMail normalParsed =
            new ParsedMail(
                sourceMessage2,
                normalMail
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
    }
}
