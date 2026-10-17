package ru.itmo.wmp.tasks.application.deck;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.wmp.tasks.application.deck.repository.DeckBoardRepository;
import ru.itmo.wmp.tasks.application.deck.repository.DeckStackRepository;
import ru.itmo.wmp.tasks.domain.TaskStatus;
import ru.itmo.wmp.tasks.domain.deck.DeckBoard;
import ru.itmo.wmp.tasks.domain.deck.DeckStack;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeckBoardService {
    private static final Map<TaskStatus, String> COLUMNS = new LinkedHashMap<>(Map.of(
        TaskStatus.NEW, "Новые",
        TaskStatus.IN_PROGRESS, "В работе",
        TaskStatus.ON_APPROVAL, "На согласовании",
        TaskStatus.DONE, "Готово",
        TaskStatus.CANCELLED, "Отменено"
    ));

    private static final TaskStatus[] COLUMN_ORDER = {
        TaskStatus.NEW,
        TaskStatus.IN_PROGRESS,
        TaskStatus.ON_APPROVAL,
        TaskStatus.DONE,
        TaskStatus.CANCELLED
    };

    private final DeckGateway deckGateway;
    private final DeckBoardRepository boardRepository;
    private final DeckStackRepository stackRepository;

    @Transactional
    public DeckBoard ensureBoard(DeckBoard board, String organizationCode) {
        if (board.getDeckBoardId() == null) {
            board.setDeckBoardId(deckGateway.createBoard(board.getTitle()));

            boardRepository.save(board);

            shareWithGroup(board, organizationCode);
        }

        ensureStacks(board);

        return board;
    }

    @Transactional(readOnly = true)
    public Long stackOf(DeckBoard board, TaskStatus status) {
        return stackRepository.findByBoardRowIdAndStatus(board.getId(), status)
            .map(DeckStack::getDeckStackId)
            .orElse(null);
    }

    private void shareWithGroup(DeckBoard board, String organizationCode) {
        String group = board.getBlockId() == null
            ? "wmp-org-" + organizationCode
            : "wmp-block-" + organizationCode + "-" + board.getBlockId();

        try {
            deckGateway.shareBoardWithGroup(board.getDeckBoardId(), group);
        } catch (RuntimeException exception) {
            log.warn(
                "Board {} was not shared with group {}: {}",
                board.getDeckBoardId(),
                group,
                exception.getMessage()
            );
        }
    }

    private void ensureStacks(DeckBoard board) {
        for (int order = 0; order < COLUMN_ORDER.length; order++) {
            TaskStatus status = COLUMN_ORDER[order];

            if (stackRepository.findByBoardRowIdAndStatus(board.getId(), status).isPresent()) {
                continue;
            }

            Long deckStackId = deckGateway.createStack(
                board.getDeckBoardId(),
                COLUMNS.get(status),
                order
            );

            DeckStack stack = new DeckStack();

            stack.setBoardRowId(board.getId());
            stack.setStatus(status);
            stack.setDeckStackId(deckStackId);

            stackRepository.save(stack);
        }
    }
}
