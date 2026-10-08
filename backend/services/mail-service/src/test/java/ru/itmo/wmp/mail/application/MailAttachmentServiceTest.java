package ru.itmo.wmp.mail.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.wmp.mail.application.attachment.MailAttachmentService;
import ru.itmo.wmp.mail.application.repository.MailAttachmentRepository;
import ru.itmo.wmp.mail.application.storage.StorageService;
import ru.itmo.wmp.mail.domain.MailAttachment;
import ru.itmo.wmp.mail.domain.MailMessage;
import ru.itmo.wmp.mail.infrastructure.imap.ParsedAttachment;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MailAttachmentServiceTest {
    private MailMessage mailMessage;
    private InputStream inputStream;
    private ParsedAttachment attachment;

    @Mock
    private StorageService storageService;

    @Mock
    private MailAttachmentRepository mailAttachmentRepository;

    @InjectMocks
    private MailAttachmentService mailAttachmentService;

    @BeforeEach
    void setUp() {
        mailMessage = new MailMessage();

        inputStream = new ByteArrayInputStream("file content".getBytes());

        attachment = new ParsedAttachment(
            "document.pdf",
            "application/pdf",
            12L,
            inputStream
        );
    }

    @Test
    void shouldUploadAndSaveAttachment() {
        String storageKey = "mail/1/document.pdf";

        when(storageService.upload(
            "document.pdf",
            inputStream,
            12L,
            "application/pdf"
        )).thenReturn(storageKey);

        MailAttachment savedAttachment = new MailAttachment();

        when(mailAttachmentRepository.save(any(MailAttachment.class)))
            .thenReturn(savedAttachment);

        MailAttachment result = mailAttachmentService.save(
            mailMessage,
            attachment
        );

        assertSame(savedAttachment, result);

        ArgumentCaptor<MailAttachment> captor = ArgumentCaptor.forClass(MailAttachment.class);

        verify(mailAttachmentRepository).save(captor.capture());

        MailAttachment mailAttachment = captor.getValue();

        assertSame(mailMessage, mailAttachment.getMailMessage());
        assertEquals("document.pdf", mailAttachment.getFilename());
        assertEquals("application/pdf", mailAttachment.getContentType());
        assertEquals(12L, mailAttachment.getSize());
        assertEquals(storageKey, mailAttachment.getStorageKey());

        verify(storageService).upload(
            "document.pdf",
            inputStream,
            12L,
            "application/pdf"
        );

        verify(storageService, never())
            .delete(anyString());
    }

    @Test
    void shouldDeleteStorageObjectWhenRepositorySaveFails() {
        String storageKey = "mail/1/document.pdf";

        when(storageService.upload(
            "document.pdf",
            inputStream,
            12L,
            "application/pdf"
        )).thenReturn(storageKey);

        RuntimeException exception = new RuntimeException("database error");

        when(mailAttachmentRepository.save(any(MailAttachment.class)))
            .thenThrow(exception);

        RuntimeException thrown = assertThrows(
            RuntimeException.class,
            () -> mailAttachmentService.save(mailMessage, attachment)
        );

        assertSame(exception, thrown);

        verify(storageService).delete(storageKey);
    }

    @Test
    void shouldNotSaveAttachmentWhenStorageUploadFails() {
        RuntimeException exception = new RuntimeException("storage error");

        when(storageService.upload(
            "document.pdf",
            inputStream,
            12L,
            "application/pdf"
        )).thenThrow(exception);

        RuntimeException thrown = assertThrows(
            RuntimeException.class,
            () -> mailAttachmentService.save(mailMessage, attachment)
        );

        assertSame(exception, thrown);

        verify(mailAttachmentRepository, never())
            .save(any(MailAttachment.class));

        verify(storageService, never())
            .delete(anyString());
    }
}
