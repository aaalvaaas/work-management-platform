package ru.itmo.wmp.platform.dto.organization;

import ru.itmo.wmp.platform.domain.organization.BlockStatus;

import java.time.LocalDateTime;

public record BlockResponse(
    Long id,
    Long organizationId,
    String name,
    BlockStatus status,
    String managerIsuNumber,
    LocalDateTime createdAt
) {
}
