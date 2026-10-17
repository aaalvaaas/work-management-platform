package ru.itmo.wmp.tasks.application.deck.repository;

import ru.itmo.wmp.tasks.domain.deck.DeckCard;
import ru.itmo.wmp.tasks.domain.deck.DeckSyncStatus;

import java.util.List;
import java.util.Optional;

public interface DeckCardRepository {
    Optional<DeckCard> findByTaskId(Long taskId);

    Optional<DeckCard> findByDeckCardId(Long deckCardId);

    List<DeckCard> findBySyncStatusIn(List<DeckSyncStatus> statuses);

    List<DeckCard> findByDeckBoardId(Long deckBoardId);

    DeckCard save(DeckCard card);
}
