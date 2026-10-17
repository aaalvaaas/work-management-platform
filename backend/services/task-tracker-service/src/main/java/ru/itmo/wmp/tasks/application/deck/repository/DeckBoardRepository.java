package ru.itmo.wmp.tasks.application.deck.repository;

import ru.itmo.wmp.tasks.domain.deck.DeckBoard;

import java.util.List;
import java.util.Optional;

public interface DeckBoardRepository {
    Optional<DeckBoard> findByOrganizationIdAndBlockId(Long organizationId, Long blockId);

    List<DeckBoard> findAll();

    DeckBoard save(DeckBoard board);
}
