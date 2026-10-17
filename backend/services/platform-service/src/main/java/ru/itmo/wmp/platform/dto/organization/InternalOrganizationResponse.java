package ru.itmo.wmp.platform.dto.organization;

public record InternalOrganizationResponse(
    Long id,
    String code,
    String name,
    String status
) {
}
