package ru.itmo.wmp.mail.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.wmp.mail.application.archive.MailParseArchive;
import ru.itmo.wmp.mail.application.archive.ParseRecord;
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

    @Mock
    private MailParseArchive mailParseArchive;

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
    void shouldStoreWhatTheParserFoundBeforePublishing() {
        MailMessage mail = mail();

        when(taskDraftParser.parse(mail)).thenReturn(draft());
        when(taskDraftParser.name()).thenReturn("RULES");
        when(taskDraftParser.version()).thenReturn("v1");

        mailProcessor.process(mail);

        ArgumentCaptor<ParseRecord> captor = ArgumentCaptor.forClass(ParseRecord.class);

        verify(mailParseArchive).record(captor.capture());

        ParseRecord stored = captor.getValue();

        assertEquals("test-001", stored.messageId());
        assertEquals("RULES", stored.parser());
        assertEquals("v1", stored.parserVersion());
        assertEquals("Закупка", stored.title());
        assertTrue(stored.requiresApproval());
        assertEquals("СОГЛАСОВАНИЕ: закупка", stored.details().get("subject"));
    }

    @Test
    void shouldPublishEvenWhenTheParseResultCannotBeStored() {
        MailMessage mail = mail();

        when(taskDraftParser.parse(mail)).thenReturn(draft());

        doThrow(new RuntimeException("Mongo is down"))
            .when(mailParseArchive)
            .record(any());

        mailProcessor.process(mail);

        verify(mailEventPublisher).publishParsed(any());

        verify(mailService).markAsProcessed(mail.getMessageId());
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
