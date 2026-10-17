package ru.itmo.wmp.tasks.application.deck;

import java.util.List;

public interface DeckGateway {
    boolean isEnabled();

    Long createBoard(String title);

    Long createStack(Long boardId, String title, int order);

    DeckCardView createCard(Long boardId, Long stackId, DeckCardContent content);

    DeckCardView updateCard(Long boardId, Long stackId, Long cardId, DeckCardContent content);

    DeckCardView moveCard(Long boardId, Long stackId, Long cardId, Long targetStackId);

    void assignUser(Long boardId, Long stackId, Long cardId, String nextcloudUid);

    void unassignUser(Long boardId, Long stackId, Long cardId, String nextcloudUid);

    List<DeckCardView> fetchCards(Long boardId);

    void shareBoardWithGroup(Long boardId, String groupId);
}
