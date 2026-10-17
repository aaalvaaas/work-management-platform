package ru.itmo.wmp.tasks.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateTaskRequest(
    Long blockId,

    @Size(max = 255, message = "Title must be at most 255 characters")
    String title,

    @Size(max = 10000, message = "Description must be at most 10000 characters")
    String description,

    @Pattern(regexp = "[1-9][0-9]{5}", message = "ISU number must contain 6 digits")
    String assigneeIsuNumber,

    LocalDate dueDate,

    boolean requiresApproval
) {
}
