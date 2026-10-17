package ru.itmo.wmp.tasks.dto;

import ru.itmo.wmp.tasks.domain.TaskSource;
import ru.itmo.wmp.tasks.domain.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponse(
    Long id,
    Long organizationId,
    Long blockId,
    String title,
    String description,
    TaskStatus status,
    boolean requiresApproval,
    TaskSource source,
    Long authorUserId,
    String requesterEmail,
    Long assigneeUserId,
    String assigneeIsuNumber,
    LocalDate dueDate,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    DeckSyncResponse deckSync
) {
}
