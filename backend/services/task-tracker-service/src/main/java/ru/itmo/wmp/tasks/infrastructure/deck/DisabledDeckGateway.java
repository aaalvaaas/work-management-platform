package ru.itmo.wmp.tasks.infrastructure.deck;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.itmo.wmp.tasks.application.deck.DeckCardContent;
import ru.itmo.wmp.tasks.application.deck.DeckCardView;
import ru.itmo.wmp.tasks.application.deck.DeckGateway;

import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(
    prefix = "deck",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true
)
public class DisabledDeckGateway implements DeckGateway {
    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public Long createBoard(String title) {
        return skip("create board " + title);
    }

    @Override
    public Long createStack(Long boardId, String title, int order) {
        return skip("create stack " + title);
    }

    @Override
    public DeckCardView createCard(Long boardId, Long stackId, DeckCardContent content) {
        skip("create card " + content.title());

        return null;
    }

    @Override
    public DeckCardView updateCard(
        Long boardId,
        Long stackId,
        Long cardId,
        DeckCardContent content
    ) {
        skip("update card " + cardId);

        return null;
    }

    @Override
    public DeckCardView moveCard(Long boardId, Long stackId, Long cardId, Long targetStackId) {
        skip("move card " + cardId);

        return null;
    }

    @Override
    public void assignUser(Long boardId, Long stackId, Long cardId, String nextcloudUid) {
        skip("assign " + nextcloudUid + " to card " + cardId);
    }

    @Override
    public void unassignUser(Long boardId, Long stackId, Long cardId, String nextcloudUid) {
        skip("unassign " + nextcloudUid + " from card " + cardId);
    }

    @Override
    public List<DeckCardView> fetchCards(Long boardId) {
        skip("fetch cards of board " + boardId);

        return List.of();
    }

    @Override
    public void shareBoardWithGroup(Long boardId, String groupId) {
        skip("share board " + boardId + " with group " + groupId);
    }

    private Long skip(String action) {
        log.debug("Deck integration is disabled, skipping: {}", action);

        return null;
    }
}
