package ru.itmo.wmp.tasks.api.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.itmo.wmp.tasks.application.deck.DeckProjectionService;
import ru.itmo.wmp.tasks.domain.Task;
import ru.itmo.wmp.tasks.domain.deck.DeckCard;
import ru.itmo.wmp.tasks.dto.DeckSyncResponse;
import ru.itmo.wmp.tasks.dto.TaskResponse;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TaskMapper {
    private final DeckProjectionService deckProjectionService;

    public TaskResponse toResponse(Task task) {
        return new TaskResponse(
            task.getId(),
            task.getOrganizationId(),
            task.getBlockId(),
            task.getTitle(),
            task.getDescription(),
            task.getStatus(),
            task.isRequiresApproval(),
            task.getSource(),
            task.getAuthorUserId(),
            task.getAssigneeUserId(),
            task.getAssigneeIsuNumber(),
            task.getDueDate(),
            task.getCreatedAt(),
            task.getUpdatedAt(),
            deckSync(task)
        );
    }

    private DeckSyncResponse deckSync(Task task) {
        return deckProjectionService.cardOf(task.getId())
            .map(this::toDeckSync)
            .orElse(null);
    }

    private DeckSyncResponse toDeckSync(DeckCard card) {
        return new DeckSyncResponse(
            card.getSyncStatus(),
            card.getDeckCardId(),
            card.getLastError(),
            card.getLastSyncedAt()
        );
    }

    public List<TaskResponse> toResponses(List<Task> tasks) {
        return tasks.stream()
            .map(this::toResponse)
            .toList();
    }
}
