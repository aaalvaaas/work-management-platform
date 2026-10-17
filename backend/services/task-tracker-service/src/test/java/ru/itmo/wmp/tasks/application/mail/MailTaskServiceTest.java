package ru.itmo.wmp.tasks.application.mail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.wmp.tasks.application.deck.DeckProjectionService;
import ru.itmo.wmp.tasks.application.repository.TaskRepository;
import ru.itmo.wmp.tasks.domain.Task;
import ru.itmo.wmp.tasks.domain.TaskSource;
import ru.itmo.wmp.tasks.domain.TaskStatus;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MailTaskServiceTest {
    @Mock
    private TaskRepository taskRepository;

    @Mock
    private DeckProjectionService deckProjectionService;

    @InjectMocks
    private MailTaskService mailTaskService;

    @Test
    void shouldCreateTaskOutOfTheEvent() {
        when(taskRepository.findBySourceAndSourceReference(TaskSource.MAIL, "mail-001"))
            .thenReturn(Optional.empty());

        when(taskRepository.save(any(Task.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        mailTaskService.createFromMail(event(true));

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);

        verify(taskRepository).save(captor.capture());

        Task task = captor.getValue();

        assertEquals(10L, task.getOrganizationId());
        assertEquals("Закупка ноутбуков", task.getTitle());
        assertEquals(TaskStatus.NEW, task.getStatus());
        assertEquals(TaskSource.MAIL, task.getSource());
        assertEquals("mail-001", task.getSourceReference());
        assertEquals("ivanov@niuitmo.ru", task.getRequesterEmail());
        assertTrue(task.isRequiresApproval());
        assertNull(task.getAuthorUserId());
        assertNull(task.getAssigneeUserId());
        assertNull(task.getBlockId());
        assertTrue(task.getDescription().contains("Нужны три ноутбука"));
        assertTrue(task.getDescription().contains("ivanov@niuitmo.ru"));
    }

    @Test
    void shouldNotCreateASecondTaskForTheSameMail() {
        Task existing = new Task();

        existing.setId(7L);

        when(taskRepository.findBySourceAndSourceReference(TaskSource.MAIL, "mail-001"))
            .thenReturn(Optional.of(existing));

        Optional<Task> result = mailTaskService.createFromMail(event(false));

        assertEquals(existing, result.orElseThrow());

        verify(taskRepository, never()).save(any());
    }

    @Test
    void shouldIgnoreAMailWithoutAnOrganization() {
        ParsedMailEvent event = new ParsedMailEvent(
            "mail-002",
            null,
            null,
            "ivanov@niuitmo.ru",
            "Закупка",
            LocalDateTime.now(),
            "Закупка",
            "текст",
            false
        );

        assertTrue(mailTaskService.createFromMail(event).isEmpty());

        verify(taskRepository, never()).save(any());
    }

    @Test
    void shouldKeepTheTaskWhenTheProjectionFails() {
        Task task = new Task();

        task.setId(7L);

        doThrow(new IllegalStateException("Platform is down"))
            .when(deckProjectionService)
            .markPending(task);

        assertDoesNotThrow(() -> mailTaskService.project(task));
    }

    private ParsedMailEvent event(boolean requiresApproval) {
        return new ParsedMailEvent(
            "mail-001",
            10L,
            1L,
            "ivanov@niuitmo.ru",
            "СОГЛАСОВАНИЕ: закупка ноутбуков",
            LocalDateTime.of(2026, 10, 17, 10, 0),
            "Закупка ноутбуков",
            "Нужны три ноутбука",
            requiresApproval
        );
    }
}
