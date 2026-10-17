package ru.itmo.wmp.tasks.infrastructure.persistance.deck;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.wmp.tasks.domain.TaskStatus;
import ru.itmo.wmp.tasks.domain.deck.DeckStack;

import java.util.List;
import java.util.Optional;

public interface JpaDeckStackRepository extends JpaRepository<DeckStack, Long> {
    List<DeckStack> findByBoardRowId(Long boardRowId);

    Optional<DeckStack> findByBoardRowIdAndStatus(Long boardRowId, TaskStatus status);
}
