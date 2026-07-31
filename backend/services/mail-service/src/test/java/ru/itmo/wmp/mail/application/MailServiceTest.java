package ru.itmo.wmp.mail.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.wmp.mail.domain.MailMessage;
import ru.itmo.wmp.mail.domain.MailProcessingStatus;
import ru.itmo.wmp.mail.domain.MailRepository;
import ru.itmo.wmp.mail.exception.DuplicateMailException;
import ru.itmo.wmp.mail.exception.InvalidMailStatusTransitionException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MailServiceTest {
    @Mock
    private MailRepository mailRepository;

    @InjectMocks
    private MailService mailService;

    @Test
    void shouldReceiveMailSuccessfully() {
        MailMessage mail = new MailMessage();

        mail.setMessageId("test-001");
        mail.setSenderEmail("sender@test.ru");
        mail.setRecipientEmail("recipient@test.ru");
        mail.setSubject("test subject");
        mail.setPlainTextBody("test body");

        when(mailRepository.existsByMessageId("test-001"))
            .thenReturn(false);

        when(mailRepository.save(mail))
            .thenReturn(mail);

        MailMessage result = mailService.receiveMail(mail);

        assertNotNull(result);

        assertEquals("test-001", result.getMessageId());
        assertEquals(MailProcessingStatus.RECEIVED, result.getProcessingStatus());
        assertNotNull(result.getReceivedAt());

        verify(mailRepository)
            .existsByMessageId("test-001");

        verify(mailRepository)
            .save(mail);
    }

    @Test
    void shouldThrowExceptionWhenMailAlreadyExists() {
        MailMessage mail = new MailMessage();

        mail.setMessageId("test-001");

        when(mailRepository.existsByMessageId("test-001"))
            .thenReturn(true);

        assertThrows(
            DuplicateMailException.class,
            () -> mailService.receiveMail(mail)
        );

        verify(mailRepository, never())
            .save(any());
    }

    @Test
    void shouldMarkMailAsProcessing() {
        MailMessage mail = new MailMessage();

        mail.setMessageId("test-001");
        mail.setProcessingStatus(MailProcessingStatus.RECEIVED);

        when(mailRepository.findByMessageId("test-001"))
            .thenReturn(Optional.of(mail));

        when(mailRepository.save(mail))
            .thenReturn(mail);

        MailMessage result = mailService.markAsProcessing("test-001");

        assertEquals(MailProcessingStatus.PROCESSING, result.getProcessingStatus());

        verify(mailRepository)
            .findByMessageId("test-001");

        verify(mailRepository)
            .save(mail);
    }

    @Test
    void shouldMarkMailAsProcessed() {
        MailMessage mail = new MailMessage();

        mail.setMessageId("test-001");
        mail.setProcessingStatus(MailProcessingStatus.PROCESSING);

        when(mailRepository.findByMessageId("test-001"))
            .thenReturn(Optional.of(mail));

        when(mailRepository.save(mail))
            .thenReturn(mail);

        MailMessage result = mailService.markAsProcessed("test-001");

        assertEquals(MailProcessingStatus.PROCESSED, result.getProcessingStatus());
        assertNotNull(result.getProcessedAt());

        verify(mailRepository)
            .findByMessageId("test-001");

        verify(mailRepository)
            .save(mail);
    }

    @Test
    void shouldMarkMailAsFailed() {
        MailMessage mail = new MailMessage();

        mail.setMessageId("test-001");
        mail.setProcessingStatus(MailProcessingStatus.PROCESSING);

        when(mailRepository.findByMessageId("test-001"))
            .thenReturn(Optional.of(mail));

        when(mailRepository.save(mail))
            .thenReturn(mail);

        MailMessage result = mailService.markAsFailed("test-001");

        assertEquals(MailProcessingStatus.FAILED, result.getProcessingStatus());
        assertNotNull(result.getProcessedAt());

        verify(mailRepository)
            .findByMessageId("test-001");

        verify(mailRepository)
            .save(mail);
    }

    @Test
    void shouldRejectReceivedToProcessedTransition() {
        MailMessage mail = new MailMessage();

        mail.setMessageId("test-001");
        mail.setProcessingStatus(MailProcessingStatus.RECEIVED);

        when(mailRepository.findByMessageId("test-001"))
            .thenReturn(Optional.of(mail));

        assertThrows(
            InvalidMailStatusTransitionException.class,
            () -> mailService.markAsProcessed("test-001")
        );

        verify(mailRepository, never())
            .save(any());
    }

    @Test
    void shouldRejectReceivedToFailedTransition() {
        MailMessage mail = new MailMessage();

        mail.setMessageId("test-001");
        mail.setProcessingStatus(MailProcessingStatus.RECEIVED);

        when(mailRepository.findByMessageId("test-001"))
            .thenReturn(Optional.of(mail));

        assertThrows(
            InvalidMailStatusTransitionException.class,
            () -> mailService.markAsFailed("test-001")
        );

        verify(mailRepository, never())
            .save(any());
    }

    @Test
    void shouldRejectProcessedToProcessingTransition() {
        MailMessage mail = new MailMessage();

        mail.setMessageId("test-001");
        mail.setProcessingStatus(MailProcessingStatus.PROCESSED);

        when(mailRepository.findByMessageId("test-001"))
            .thenReturn(Optional.of(mail));

        assertThrows(
            InvalidMailStatusTransitionException.class,
            () -> mailService.markAsProcessing("test-001")
        );

        verify(mailRepository, never())
            .save(any());
    }

    @Test
    void shouldRejectFailedToProcessingTransition() {
        MailMessage mail = new MailMessage();

        mail.setMessageId("test-001");
        mail.setProcessingStatus(MailProcessingStatus.FAILED);

        when(mailRepository.findByMessageId("test-001"))
            .thenReturn(Optional.of(mail));

        assertThrows(
            InvalidMailStatusTransitionException.class,
            () -> mailService.markAsProcessing("test-001")
        );

        verify(mailRepository, never())
            .save(any());
    }
}
