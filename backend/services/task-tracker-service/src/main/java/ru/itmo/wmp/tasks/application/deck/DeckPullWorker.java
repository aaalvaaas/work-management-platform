package ru.itmo.wmp.tasks.application.deck;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.itmo.wmp.tasks.application.deck.repository.DeckBoardRepository;
import ru.itmo.wmp.tasks.application.deck.repository.DeckCardRepository;
import ru.itmo.wmp.tasks.application.deck.repository.DeckStackRepository;
import ru.itmo.wmp.tasks.application.repository.TaskRepository;
import ru.itmo.wmp.tasks.domain.Task;
import ru.itmo.wmp.tasks.domain.TaskStatus;
import ru.itmo.wmp.tasks.domain.TaskStatusTransition;
import ru.itmo.wmp.tasks.domain.deck.DeckBoard;
import ru.itmo.wmp.tasks.domain.deck.DeckCard;
import ru.itmo.wmp.tasks.domain.deck.DeckStack;
import ru.itmo.wmp.tasks.domain.deck.DeckSyncStatus;

import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeckPullWorker {
    private final DeckGateway deckGateway;
    private final DeckBoardRepository boardRepository;
    private final DeckStackRepository stackRepository;
    private final DeckCardRepository cardRepository;
    private final TaskRepository taskRepository;

    @Scheduled(fixedDelayString = "${deck.pull-interval:60s}")
    public void pull() {
        if (!deckGateway.isEnabled()) {
            return;
        }

        for (DeckBoard board : boardRepository.findAll()) {
            if (board.getDeckBoardId() == null) {
                continue;
            }

            try {
                pullBoard(board);
            } catch (RuntimeException exception) {
                log.warn(
                    "Reading board {} from Deck failed: {}",
                    board.getDeckBoardId(),
                    exception.getMessage()
                );
            }
        }
    }

    public void pullBoard(DeckBoard board) {
        for (DeckCardView view : deckGateway.fetchCards(board.getDeckBoardId())) {
            cardRepository.findByDeckCardId(view.cardId())
                .filter(card -> card.getSyncStatus() == DeckSyncStatus.SYNCED)
                .filter(card -> isNewer(card, view))
                .ifPresent(card -> apply(board, card, view));
        }
    }

    private boolean isNewer(DeckCard card, DeckCardView view) {
        return view.lastModified() != null
            && !Objects.equals(view.lastModified(), card.getDeckLastModified());
    }

    private void apply(DeckBoard board, DeckCard card, DeckCardView view) {
        Task task = taskRepository.findById(card.getTaskId()).orElse(null);

        if (task == null) {
            return;
        }

        if (view.title() != null) {
            task.setTitle(view.title());
        }

        task.setDescription(view.description());
        task.setDueDate(view.dueDate());

        boolean reverted = applyStatus(board, card, view, task);

        taskRepository.save(task);

        card.setDeckLastModified(view.lastModified());
        card.setSyncStatus(reverted ? DeckSyncStatus.PENDING : DeckSyncStatus.SYNCED);

        cardRepository.save(card);

        log.info("Task {} updated from Deck card {}", task.getId(), view.cardId());
    }

    private boolean applyStatus(DeckBoard board, DeckCard card, DeckCardView view, Task task) {
        Optional<TaskStatus> moved = stackRepository.findByBoardRowId(board.getId())
            .stream()
            .filter(stack -> Objects.equals(stack.getDeckStackId(), view.stackId()))
            .map(DeckStack::getStatus)
            .findFirst();

        if (moved.isEmpty() || moved.get() == task.getStatus()) {
            return false;
        }

        TaskStatus target = moved.get();

        boolean allowed = TaskStatusTransition.isAllowed(task.getStatus(), target)
            && !TaskStatusTransition.needsApproval(task, target);

        if (!allowed) {
            log.info(
                "Deck card {} was moved to {} which task {} may not enter, moving it back",
                view.cardId(),
                target,
                task.getId()
            );

            return true;
        }

        task.setStatus(target);
        card.setDeckStackId(view.stackId());

        return false;
    }
}
