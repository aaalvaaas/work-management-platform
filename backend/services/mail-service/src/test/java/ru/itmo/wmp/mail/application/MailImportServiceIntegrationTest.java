package ru.itmo.wmp.mail.application;

import jakarta.mail.Message;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.itmo.wmp.mail.application.repository.MailAttachmentRepository;
import ru.itmo.wmp.mail.application.repository.MailRepository;
import ru.itmo.wmp.mail.application.storage.StorageService;
import ru.itmo.wmp.mail.domain.MailAttachment;
import ru.itmo.wmp.mail.domain.MailMessage;
import ru.itmo.wmp.mail.domain.MailProcessingStatus;
import ru.itmo.wmp.mail.infrastructure.imap.ImapMailClient;
import ru.itmo.wmp.mail.infrastructure.imap.ParsedAttachment;
import ru.itmo.wmp.mail.infrastructure.imap.ParsedMail;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
public class MailImportServiceIntegrationTest {
    @Autowired
    private MailImportService mailImportService;

    @Autowired
    private MailRepository mailRepository;

    @Autowired
    private MailAttachmentRepository mailAttachmentRepository;

    @Autowired
    private MailService mailService;

    @MockitoBean
    private ImapMailClient imapMailClient;

    @MockitoBean
    private StorageService storageService;

    @MockitoBean
    private MailProcessor mailProcessor;

    @Mock
    private Message sourceMessage;

    @BeforeEach
    void cleanDatabase() {
        mailRepository.deleteAll();
    }

    @Test
    void shouldPersistMailAndAttachmentsDuringImport() {
        MailMessage mailMessage = new MailMessage();

        mailMessage.setMessageId("integration-attachment-001");
        mailMessage.setSenderEmail("sender@test.ru");
        mailMessage.setRecipientEmail("recipient@test.ru");
        mailMessage.setSubject("Test subject");
        mailMessage.setPlainTextBody("Test body");
        mailMessage.setReceivedAt(LocalDateTime.now());
        mailMessage.setProcessingStatus(MailProcessingStatus.RECEIVED);

        ParsedAttachment attachment = new ParsedAttachment(
            "document.pdf",
            "application/pdf",
            1024L,
            new ByteArrayInputStream("file content".getBytes())
        );

        ParsedMail parsedMail = new ParsedMail(
            sourceMessage,
            mailMessage,
            List.of(attachment)
        );

        when(imapMailClient.fetchUnread())
            .thenReturn(List.of(parsedMail));

        when(storageService.upload(
            attachment.filename(),
            attachment.inputStream(),
            attachment.size(),
            attachment.contentType()
        )).thenReturn("mail/1/document.pdf");

        MailImportResult result = mailImportService.importUnreadMails();

        assertEquals(1, result.imported());
        assertEquals(0, result.skipped());

        MailMessage savedMail = mailRepository.findByMessageId("integration-attachment-001").orElseThrow();

        assertNotNull(savedMail.getId());
        assertEquals("Test subject", savedMail.getSubject());

        List<MailAttachment> attachments = mailAttachmentRepository.findAllByMailMessageId(savedMail.getId());

        assertEquals(1, attachments.size());

        MailAttachment savedAttachment = attachments.getFirst();

        assertNotNull(savedAttachment.getId());
        assertEquals(savedMail.getId(), savedAttachment.getMailMessage().getId());
        assertEquals("document.pdf", savedAttachment.getFilename());
        assertEquals(1024L, savedAttachment.getSize());
        assertEquals("application/pdf", savedAttachment.getContentType());
        assertEquals("mail/1/document.pdf", savedAttachment.getStorageKey());

        verify(mailProcessor)
            .process(savedMail);

        verify(imapMailClient)
            .markAsRead(sourceMessage);
    }
}
