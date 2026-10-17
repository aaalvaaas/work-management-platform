package ru.itmo.wmp.tasks.infrastructure.persistance.deck;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.wmp.tasks.domain.deck.DeckCard;
import ru.itmo.wmp.tasks.domain.deck.DeckSyncStatus;

import java.util.List;
import java.util.Optional;

public interface JpaDeckCardRepository extends JpaRepository<DeckCard, Long> {
    Optional<DeckCard> findByTaskId(Long taskId);

    Optional<DeckCard> findByDeckCardId(Long deckCardId);

    List<DeckCard> findBySyncStatusIn(List<DeckSyncStatus> statuses);

    List<DeckCard> findByDeckBoardId(Long deckBoardId);
}
