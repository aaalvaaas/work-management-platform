package ru.itmo.wmp.platform.application.registration;

import java.time.LocalDateTime;

public record RegistrationStart(
    String email,
    LocalDateTime expiresAt
) {
}
