package ru.itmo.wmp.platform.dto.organization;

import ru.itmo.wmp.platform.domain.organization.ParticipantStatus;

public record ParticipantResponse(
    Long id,
    String isuNumber,
    String lastName,
    String firstName,
    String patronymic,
    String email,
    String position,
    ParticipantStatus status,
    boolean registered
) {
}
