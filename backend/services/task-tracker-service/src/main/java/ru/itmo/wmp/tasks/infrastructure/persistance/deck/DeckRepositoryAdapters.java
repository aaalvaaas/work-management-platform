package ru.itmo.wmp.tasks.infrastructure.persistance.deck;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.itmo.wmp.tasks.application.deck.repository.DeckBoardRepository;
import ru.itmo.wmp.tasks.application.deck.repository.DeckCardRepository;
import ru.itmo.wmp.tasks.application.deck.repository.DeckStackRepository;
import ru.itmo.wmp.tasks.domain.TaskStatus;
import ru.itmo.wmp.tasks.domain.deck.DeckBoard;
import ru.itmo.wmp.tasks.domain.deck.DeckCard;
import ru.itmo.wmp.tasks.domain.deck.DeckStack;
import ru.itmo.wmp.tasks.domain.deck.DeckSyncStatus;

import java.util.List;
import java.util.Optional;

public final class DeckRepositoryAdapters {
    private DeckRepositoryAdapters() {
    }

    @Repository
    @RequiredArgsConstructor
    public static class BoardAdapter implements DeckBoardRepository {
        private final JpaDeckBoardRepository boardRepository;

        @Override
        public Optional<DeckBoard> findByOrganizationIdAndBlockId(
            Long organizationId,
            Long blockId
        ) {
            return boardRepository.findForOrganizationAndBlock(organizationId, blockId);
        }

        @Override
        public List<DeckBoard> findAll() {
            return boardRepository.findAll();
        }

        @Override
        public DeckBoard save(DeckBoard board) {
            return boardRepository.save(board);
        }
    }

    @Repository
    @RequiredArgsConstructor
    public static class StackAdapter implements DeckStackRepository {
        private final JpaDeckStackRepository stackRepository;

        @Override
        public List<DeckStack> findByBoardRowId(Long boardRowId) {
            return stackRepository.findByBoardRowId(boardRowId);
        }

        @Override
        public Optional<DeckStack> findByBoardRowIdAndStatus(Long boardRowId, TaskStatus status) {
            return stackRepository.findByBoardRowIdAndStatus(boardRowId, status);
        }

        @Override
        public DeckStack save(DeckStack stack) {
            return stackRepository.save(stack);
        }
    }

    @Repository
    @RequiredArgsConstructor
    public static class CardAdapter implements DeckCardRepository {
        private final JpaDeckCardRepository cardRepository;

        @Override
        public Optional<DeckCard> findByTaskId(Long taskId) {
            return cardRepository.findByTaskId(taskId);
        }

        @Override
        public Optional<DeckCard> findByDeckCardId(Long deckCardId) {
            return cardRepository.findByDeckCardId(deckCardId);
        }

        @Override
        public List<DeckCard> findBySyncStatusIn(List<DeckSyncStatus> statuses) {
            return cardRepository.findBySyncStatusIn(statuses);
        }

        @Override
        public List<DeckCard> findByDeckBoardId(Long deckBoardId) {
            return cardRepository.findByDeckBoardId(deckBoardId);
        }

        @Override
        public DeckCard save(DeckCard card) {
            return cardRepository.save(card);
        }
    }
}
