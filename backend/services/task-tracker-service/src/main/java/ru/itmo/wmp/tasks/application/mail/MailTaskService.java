package ru.itmo.wmp.tasks.application.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.wmp.tasks.application.deck.DeckProjectionService;
import ru.itmo.wmp.tasks.application.repository.TaskRepository;
import ru.itmo.wmp.tasks.domain.Task;
import ru.itmo.wmp.tasks.domain.TaskSource;
import ru.itmo.wmp.tasks.domain.TaskStatus;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailTaskService {
    private final TaskRepository taskRepository;
    private final DeckProjectionService deckProjectionService;

    @Transactional
    public Optional<Task> createFromMail(ParsedMailEvent event) {
        if (event.organizationId() == null) {
            log.warn("Mail {} belongs to no organization, no task created", event.messageId());

            return Optional.empty();
        }

        Optional<Task> known = taskRepository.findBySourceAndSourceReference(
            TaskSource.MAIL,
            event.messageId()
        );

        if (known.isPresent()) {
            log.info("Mail {} already became task {}", event.messageId(), known.get().getId());

            return known;
        }

        Task task = new Task();

        task.setOrganizationId(event.organizationId());
        task.setTitle(event.title());
        task.setDescription(description(event));
        task.setStatus(TaskStatus.NEW);
        task.setRequiresApproval(event.requiresApproval());
        task.setSource(TaskSource.MAIL);
        task.setSourceReference(event.messageId());
        task.setRequesterEmail(event.requesterEmail());

        Task saved = taskRepository.save(task);

        log.info(
            "Mail {} of organization {} became task {}",
            event.messageId(),
            event.organizationId(),
            saved.getId()
        );

        return Optional.of(saved);
    }

    public void project(Task task) {
        try {
            deckProjectionService.markPending(task);
        } catch (RuntimeException exception) {
            log.warn(
                "Task {} from a mail is not projected to Deck yet: {}",
                task.getId(),
                exception.getMessage()
            );
        }
    }

    private String description(ParsedMailEvent event) {
        return "Письмо от %s, получено %s%n%n%s".formatted(
            event.requesterEmail(),
            event.receivedAt(),
            event.description()
        );
    }
}
