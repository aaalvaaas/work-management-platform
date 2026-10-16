package ru.itmo.wmp.platform.dto.organization;

import ru.itmo.wmp.platform.domain.organization.OrganizationStatus;

import java.time.LocalDateTime;

public record OrganizationResponse(
    Long id,
    String code,
    String name,
    OrganizationStatus status,
    String managerIsuNumber,
    LocalDateTime createdAt
) {
}
