package ru.itmo.wmp.mail.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.wmp.mail.application.event.MailEventPublisher;
import ru.itmo.wmp.mail.application.event.MailParsedEvent;
import ru.itmo.wmp.mail.application.parse.TaskDraft;
import ru.itmo.wmp.mail.application.parse.TaskDraftParser;
import ru.itmo.wmp.mail.domain.MailMessage;

import org.mockito.ArgumentCaptor;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class MailProcessorTest {
    @Mock
    private MailService mailService;

    @Mock
    private TaskDraftParser taskDraftParser;

    @Mock
    private MailEventPublisher mailEventPublisher;

    @InjectMocks
    private MailProcessor mailProcessor;

    @Test
    void shouldProcessMailSuccessfully() {
        MailMessage mail = mail();

        when(taskDraftParser.parse(mail)).thenReturn(draft());

        mailProcessor.process(mail);

        verify(mailService)
            .markAsProcessing(mail.getMessageId());

        verify(mailService)
            .markAsProcessed(mail.getMessageId());

        verify(mailService, never())
            .markAsFailed(any());
    }

    @Test
    void shouldPublishWhatWasParsedOutOfTheMail() {
        MailMessage mail = mail();

        when(taskDraftParser.parse(mail)).thenReturn(draft());

        mailProcessor.process(mail);

        ArgumentCaptor<MailParsedEvent> captor = ArgumentCaptor.forClass(MailParsedEvent.class);

        verify(mailEventPublisher).publishParsed(captor.capture());

        MailParsedEvent event = captor.getValue();

        assertEquals("test-001", event.messageId());
        assertEquals(10L, event.organizationId());
        assertEquals("Закупка", event.title());
        assertEquals("Нужны ноутбуки", event.description());
        assertEquals("ivanov@niuitmo.ru", event.requesterEmail());
        assertTrue(event.requiresApproval());
    }

    @Test
    void shouldMarkMailAsFailedWhenPublishingFails() {
        MailMessage mail = mail();

        when(taskDraftParser.parse(mail)).thenReturn(draft());

        doThrow(new RuntimeException("Broker is down"))
            .when(mailEventPublisher)
            .publishParsed(any());

        assertThrows(RuntimeException.class, () -> mailProcessor.process(mail));

        verify(mailService).markAsFailed(mail.getMessageId());

        verify(mailService, never())
            .markAsProcessed(any());
    }

    private MailMessage mail() {
        MailMessage mail = new MailMessage();

        mail.setMessageId("test-001");
        mail.setOrganizationId(10L);
        mail.setMailboxId(1L);
        mail.setSubject("СОГЛАСОВАНИЕ: закупка");

        return mail;
    }

    private TaskDraft draft() {
        return new TaskDraft("Закупка", "Нужны ноутбуки", true, "ivanov@niuitmo.ru");
    }

    @Test
    void shouldMarkMailAsFailedWhenProcessingFails() {
        MailMessage mail = mail();

        when(taskDraftParser.parse(mail)).thenReturn(draft());

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
