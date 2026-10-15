package ru.itmo.wmp.platform.dto.registration;

import java.time.LocalDateTime;

public record RegistrationStartResponse(
    String email,
    LocalDateTime expiresAt
) {
}
