package ru.itmo.wmp.platform.dto.registration;

import ru.itmo.wmp.platform.domain.registration.RegistrationRequestStatus;

import java.time.LocalDateTime;

public record RegistrationRequestResponse(
    Long id,
    Long organizationId,
    String organizationName,
    String isuNumber,
    String lastName,
    String firstName,
    String patronymic,
    String email,
    String position,
    RegistrationRequestStatus status,
    String decisionComment,
    LocalDateTime createdAt,
    LocalDateTime decidedAt
) {
}
