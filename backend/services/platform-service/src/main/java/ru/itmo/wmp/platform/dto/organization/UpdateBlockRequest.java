package ru.itmo.wmp.platform.dto.organization;

import jakarta.validation.constraints.Size;
import ru.itmo.wmp.platform.domain.organization.BlockStatus;

public record UpdateBlockRequest(
    @Size(max = 255, message = "Name must be at most 255 characters")
    String name,

    BlockStatus status
) {
}
