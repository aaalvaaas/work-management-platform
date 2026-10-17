package ru.itmo.wmp.tasks.application.deck;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.itmo.wmp.tasks.application.deck.repository.DeckBoardRepository;
import ru.itmo.wmp.tasks.application.deck.repository.DeckCardRepository;
import ru.itmo.wmp.tasks.application.repository.TaskRepository;
import ru.itmo.wmp.tasks.domain.Task;
import ru.itmo.wmp.tasks.domain.deck.DeckBoard;
import ru.itmo.wmp.tasks.domain.deck.DeckCard;
import ru.itmo.wmp.tasks.domain.deck.DeckSyncStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeckPushWorker {
    private static final int ERROR_LIMIT = 500;

    private final DeckGateway deckGateway;
    private final DeckBoardService boardService;
    private final DeckBoardRepository boardRepository;
    private final DeckCardRepository cardRepository;
    private final TaskRepository taskRepository;

    @Scheduled(fixedDelayString = "${deck.push-interval:5s}")
    public void push() {
        if (!deckGateway.isEnabled()) {
            return;
        }

        List<DeckCard> waiting = cardRepository.findBySyncStatusIn(
            List.of(DeckSyncStatus.PENDING, DeckSyncStatus.FAILED)
        );

        for (DeckCard card : waiting) {
            pushSafely(card);
        }
    }

    public void pushSafely(DeckCard card) {
        try {
            pushOne(card);
        } catch (RuntimeException exception) {
            log.warn("Deck projection failed for task {}: {}", card.getTaskId(), exception.getMessage());

            card.setSyncStatus(DeckSyncStatus.FAILED);
            card.setLastError(shorten(exception.getMessage()));

            cardRepository.save(card);
        }
    }

    private void pushOne(DeckCard card) {
        Task task = taskRepository.findById(card.getTaskId()).orElse(null);

        if (task == null) {
            card.setSyncStatus(DeckSyncStatus.DETACHED);

            cardRepository.save(card);

            return;
        }

        DeckBoard boardRow = board(task);
        DeckBoard board = boardService.ensureBoard(boardRow, boardRow.getOrganizationCode());
        Long targetStack = boardService.stackOf(board, task.getStatus());

        DeckCardContent content = new DeckCardContent(
            task.getTitle(),
            task.getDescription(),
            task.getDueDate()
        );

        DeckCardView view = card.getDeckCardId() == null
            ? deckGateway.createCard(board.getDeckBoardId(), targetStack, content)
            : updateExisting(card, board, targetStack, content);

        card.setDeckBoardId(board.getDeckBoardId());
        card.setDeckStackId(targetStack);
        card.setDeckCardId(view == null ? card.getDeckCardId() : view.cardId());

        syncAssignee(card, task, board);

        card.setSyncStatus(DeckSyncStatus.SYNCED);
        card.setLastError(null);
        card.setLastSyncedAt(LocalDateTime.now());

        if (view != null && view.lastModified() != null) {
            card.setDeckLastModified(view.lastModified());
        }

        cardRepository.save(card);
    }

    private DeckCardView updateExisting(
        DeckCard card,
        DeckBoard board,
        Long targetStack,
        DeckCardContent content
    ) {
        if (!Objects.equals(card.getDeckStackId(), targetStack)) {
            deckGateway.moveCard(
                board.getDeckBoardId(),
                card.getDeckStackId(),
                card.getDeckCardId(),
                targetStack
            );
        }

        return deckGateway.updateCard(
            board.getDeckBoardId(),
            targetStack,
            card.getDeckCardId(),
            content
        );
    }

    private void syncAssignee(DeckCard card, Task task, DeckBoard board) {
        String target = task.getAssigneeIsuNumber();

        if (Objects.equals(target, card.getAssignedUid())) {
            return;
        }

        if (card.getAssignedUid() != null) {
            deckGateway.unassignUser(
                board.getDeckBoardId(),
                card.getDeckStackId(),
                card.getDeckCardId(),
                card.getAssignedUid()
            );
        }

        if (target != null) {
            deckGateway.assignUser(
                board.getDeckBoardId(),
                card.getDeckStackId(),
                card.getDeckCardId(),
                target
            );
        }

        card.setAssignedUid(target);
    }

    private DeckBoard board(Task task) {
        return boardRepository
            .findByOrganizationIdAndBlockId(task.getOrganizationId(), task.getBlockId())
            .orElseThrow(() -> new IllegalStateException(
                "Deck board row is missing for organization " + task.getOrganizationId()
            ));
    }

    private String shorten(String message) {
        if (message == null) {
            return "unknown error";
        }

        return message.length() <= ERROR_LIMIT ? message : message.substring(0, ERROR_LIMIT);
    }
}
