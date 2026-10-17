package ru.itmo.wmp.tasks.application.deck;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.wmp.tasks.application.BlockSummary;
import ru.itmo.wmp.tasks.application.OrganizationSummary;
import ru.itmo.wmp.tasks.application.PlatformGateway;
import ru.itmo.wmp.tasks.application.deck.repository.DeckBoardRepository;
import ru.itmo.wmp.tasks.application.deck.repository.DeckCardRepository;
import ru.itmo.wmp.tasks.domain.Task;
import ru.itmo.wmp.tasks.domain.deck.DeckBoard;
import ru.itmo.wmp.tasks.domain.deck.DeckCard;
import ru.itmo.wmp.tasks.domain.deck.DeckSyncStatus;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeckProjectionService {
    private static final String BOARD_PREFIX = "WMP · ";

    private final DeckGateway deckGateway;
    private final DeckBoardRepository boardRepository;
    private final DeckCardRepository cardRepository;
    private final PlatformGateway platformGateway;

    @Transactional
    public void markPending(Task task) {
        if (!deckGateway.isEnabled()) {
            return;
        }

        boardFor(task);

        DeckCard card = cardRepository.findByTaskId(task.getId())
            .orElseGet(() -> newCard(task));

        if (card.getSyncStatus() == DeckSyncStatus.DETACHED) {
            return;
        }

        card.setSyncStatus(DeckSyncStatus.PENDING);

        cardRepository.save(card);
    }

    @Transactional(readOnly = true)
    public Optional<DeckCard> cardOf(Long taskId) {
        return cardRepository.findByTaskId(taskId);
    }

    private DeckBoard boardFor(Task task) {
        return boardRepository
            .findByOrganizationIdAndBlockId(task.getOrganizationId(), task.getBlockId())
            .orElseGet(() -> boardRepository.save(newBoard(task)));
    }

    private DeckBoard newBoard(Task task) {
        OrganizationSummary organization = platformGateway.organizationOf(task.getOrganizationId());

        DeckBoard board = new DeckBoard();

        board.setOrganizationId(task.getOrganizationId());
        board.setBlockId(task.getBlockId());
        board.setTitle(boardTitle(organization, task));

        return board;
    }

    private String boardTitle(OrganizationSummary organization, Task task) {
        if (task.getBlockId() == null) {
            return BOARD_PREFIX + organization.name();
        }

        BlockSummary block = platformGateway.blockOf(task.getOrganizationId(), task.getBlockId());

        return BOARD_PREFIX + organization.name() + " · " + block.name();
    }

    private DeckCard newCard(Task task) {
        DeckCard card = new DeckCard();

        card.setTaskId(task.getId());
        card.setSyncStatus(DeckSyncStatus.PENDING);

        return card;
    }
}
