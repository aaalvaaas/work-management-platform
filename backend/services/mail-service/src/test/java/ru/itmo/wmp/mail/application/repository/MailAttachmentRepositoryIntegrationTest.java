package ru.itmo.wmp.mail.application.repository;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.itmo.wmp.mail.domain.MailAttachment;
import ru.itmo.wmp.mail.domain.MailMessage;
import ru.itmo.wmp.mail.domain.MailProcessingStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Transactional
public class MailAttachmentRepositoryIntegrationTest {
    @Autowired
    private MailAttachmentRepository mailAttachmentRepository;

    @Autowired
    private MailRepository mailRepository;

    @BeforeEach
    void cleanDatabase() {
        mailRepository.deleteAll();
    }

    @Test
    void shouldSaveAndFindAttachmentByMailMessageId() {
        MailMessage mailMessage = new MailMessage();

        mailMessage.setMessageId("test-attachment-001");
        mailMessage.setSenderEmail("sender@test.ru");
        mailMessage.setRecipientEmail("recipient@test.ru");
        mailMessage.setSubject("Test attachment");
        mailMessage.setPlainTextBody("Test body");
        mailMessage.setReceivedAt(LocalDateTime.now());
        mailMessage.setProcessingStatus(MailProcessingStatus.RECEIVED);

        MailMessage savedMail = mailRepository.save(mailMessage);

        MailAttachment attachment = new MailAttachment();

        attachment.setMailMessage(savedMail);
        attachment.setFilename("document.pdf");
        attachment.setContentType("application/pdf");
        attachment.setSize(1024L);
        attachment.setStorageKey("mail/1/document.pdf");

        mailAttachmentRepository.save(attachment);

        List<MailAttachment> attachments = mailAttachmentRepository.findAllByMailMessageId(savedMail.getId());

        assertEquals(1, attachments.size());

        MailAttachment savedAttachment = attachments.getFirst();

        assertNotNull(savedAttachment.getId());
        assertEquals(savedMail.getId(), savedAttachment.getMailMessage().getId());
        assertEquals("document.pdf", savedAttachment.getFilename());
        assertEquals("application/pdf", savedAttachment.getContentType());
        assertEquals(1024L, savedAttachment.getSize());
        assertEquals("mail/1/document.pdf", savedAttachment.getStorageKey());
    }
}
