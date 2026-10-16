package ru.itmo.wmp.platform.dto.organization;

import jakarta.validation.constraints.NotNull;
import ru.itmo.wmp.platform.domain.organization.OrganizationStatus;

public record UpdateOrganizationStatusRequest(
    @NotNull(message = "Status is required")
    OrganizationStatus status
) {
}
