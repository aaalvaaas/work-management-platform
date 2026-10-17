package ru.itmo.wmp.tasks.dto;

import ru.itmo.wmp.tasks.domain.deck.DeckSyncStatus;

import java.time.LocalDateTime;

public record DeckSyncResponse(
    DeckSyncStatus status,
    Long deckCardId,
    String lastError,
    LocalDateTime lastSyncedAt
) {
}
