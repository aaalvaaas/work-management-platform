package ru.itmo.wmp.platform.dto.registration;

import ru.itmo.wmp.platform.domain.registration.InvitationSource;

import java.time.LocalDateTime;

public record InvitationResponse(
    Long id,
    String isuNumber,
    String email,
    Long organizationId,
    InvitationSource source,
    LocalDateTime expiresAt,
    String registrationToken
) {
}
