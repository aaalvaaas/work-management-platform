package ru.itmo.wmp.tasks.application;

import java.time.LocalDate;

public record TaskCommand(
    Long organizationId,
    Long blockId,
    String title,
    String description,
    String assigneeIsuNumber,
    LocalDate dueDate,
    boolean requiresApproval
) {
}
