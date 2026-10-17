package ru.itmo.wmp.tasks.infrastructure.persistance.deck;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.itmo.wmp.tasks.domain.deck.DeckBoard;

import java.util.Optional;

public interface JpaDeckBoardRepository extends JpaRepository<DeckBoard, Long> {
    @Query("""
        select board from DeckBoard board
        where board.organizationId = :organizationId
          and ((:blockId is null and board.blockId is null) or board.blockId = :blockId)
        """)
    Optional<DeckBoard> findForOrganizationAndBlock(
        @Param("organizationId") Long organizationId,
        @Param("blockId") Long blockId
    );
}
