package ru.itmo.wmp.tasks.dto;

import jakarta.validation.constraints.NotNull;
import ru.itmo.wmp.tasks.domain.TaskStatus;

public record ChangeTaskStatusRequest(
    @NotNull(message = "Status is required")
    TaskStatus status
) {
}
