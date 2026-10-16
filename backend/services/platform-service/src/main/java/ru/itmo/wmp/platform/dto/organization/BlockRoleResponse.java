package ru.itmo.wmp.platform.dto.organization;

import ru.itmo.wmp.platform.domain.organization.BlockRoleType;

import java.time.LocalDateTime;

public record BlockRoleResponse(
    Long blockId,
    Long userId,
    String isuNumber,
    String fullName,
    BlockRoleType role,
    LocalDateTime grantedAt
) {
}
