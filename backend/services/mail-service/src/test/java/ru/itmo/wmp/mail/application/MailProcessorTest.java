package ru.itmo.wmp.mail.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.wmp.mail.domain.MailMessage;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class MailProcessorTest {
    @Mock
    private MailService mailService;

    @InjectMocks
    private MailProcessor mailProcessor;

    @Test
    void shouldProcessMailSuccessfully() {
        MailMessage mail = new MailMessage();
        mail.setMessageId("test-001");

        mailProcessor.process(mail);

        verify(mailService)
            .markAsProcessing(mail.getMessageId());

        verify(mailService)
            .markAsProcessed(mail.getMessageId());

        verify(mailService, never())
            .markAsFailed(any());
    }

    @Test
    void shouldMarkMailAsFailedWhenProcessingFails() {
        MailMessage mail = new MailMessage();
        mail.setMessageId("test-001");

        doThrow(new RuntimeException("Processing failed"))
            .when(mailService)
            .markAsProcessed(mail.getMessageId());

        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> mailProcessor.process(mail)
        );

        assertEquals("Processing failed", exception.getMessage());

        verify(mailService)
            .markAsProcessing(mail.getMessageId());

        verify(mailService)
            .markAsFailed(mail.getMessageId());

        verify(mailService)
            .markAsProcessed(mail.getMessageId());
    }
}
