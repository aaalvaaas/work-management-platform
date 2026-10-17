package ru.itmo.wmp.tasks.application.deck;

import java.time.LocalDate;

public record DeckCardContent(
    String title,
    String description,
    LocalDate dueDate
) {
}
