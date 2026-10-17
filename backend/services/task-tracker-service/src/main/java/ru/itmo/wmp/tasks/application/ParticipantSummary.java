package ru.itmo.wmp.tasks.application;

public record ParticipantSummary(
    String isuNumber,
    Long userId,
    String fullName
) {
}
