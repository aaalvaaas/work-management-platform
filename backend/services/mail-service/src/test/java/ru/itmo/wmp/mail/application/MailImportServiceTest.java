package ru.itmo.wmp.mail.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.wmp.mail.domain.MailMessage;
import ru.itmo.wmp.mail.exception.DuplicateMailException;
import ru.itmo.wmp.mail.infrastructure.imap.ImapMailClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MailImportServiceTest {
    @Mock
    private ImapMailClient imapMailClient;

    @Mock
    private MailService mailService;

    @InjectMocks
    private MailImportService mailImportService;

    @Test
    void shouldImportUndreadMailsSuccessfully() {
        MailMessage mail1 = new MailMessage();
        mail1.setMessageId("test-001");

        MailMessage mail2 = new MailMessage();
        mail2.setMessageId("test-002");

        when(imapMailClient.fetchUnread())
            .thenReturn(List.of(mail1, mail2));

        MailImportResult result = mailImportService.importUnreadMails();

        assertEquals(2, result.imported());
        assertEquals(0, result.skipped());

        verify(imapMailClient)
            .fetchUnread();

        verify(mailService)
            .receiveMail(mail1);

        verify(mailService)
            .receiveMail(mail2);
    }

    @Test
    void shouldSkipDuplicateMails() {
        MailMessage duplicateMail = new MailMessage();
        duplicateMail.setMessageId("test-001");

        MailMessage normalMail = new MailMessage();
        normalMail.setMessageId("test-002");

        when(imapMailClient.fetchUnread())
            .thenReturn(List.of(duplicateMail, normalMail));

        doThrow(new DuplicateMailException("test-001"))
            .when(mailService)
            .receiveMail(duplicateMail);

        MailImportResult result = mailImportService.importUnreadMails();

        assertEquals(1, result.imported());
        assertEquals(1, result.skipped());

        verify(mailService)
            .receiveMail(duplicateMail);

        verify(mailService)
            .receiveMail(normalMail);
    }
}
