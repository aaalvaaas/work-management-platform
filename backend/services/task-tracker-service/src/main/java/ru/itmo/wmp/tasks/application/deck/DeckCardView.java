package ru.itmo.wmp.tasks.application.deck;

import java.time.LocalDate;
import java.util.List;

public record DeckCardView(
    Long cardId,
    Long stackId,
    String title,
    String description,
    LocalDate dueDate,
    List<String> assignedUids,
    Long lastModified,
    boolean deleted
) {
}
