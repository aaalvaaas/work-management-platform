package ru.itmo.wmp.platform.dto.organization;

import ru.itmo.wmp.platform.domain.organization.OrganizationRoleType;

import java.time.LocalDateTime;

public record OrganizationRoleResponse(
    Long userId,
    String isuNumber,
    String fullName,
    OrganizationRoleType role,
    LocalDateTime grantedAt
) {
}
