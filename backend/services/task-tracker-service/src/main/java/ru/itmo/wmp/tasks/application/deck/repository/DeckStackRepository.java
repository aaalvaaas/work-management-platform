package ru.itmo.wmp.tasks.application.deck.repository;

import ru.itmo.wmp.tasks.domain.TaskStatus;
import ru.itmo.wmp.tasks.domain.deck.DeckStack;

import java.util.List;
import java.util.Optional;

public interface DeckStackRepository {
    List<DeckStack> findByBoardRowId(Long boardRowId);

    Optional<DeckStack> findByBoardRowIdAndStatus(Long boardRowId, TaskStatus status);

    DeckStack save(DeckStack stack);
}
