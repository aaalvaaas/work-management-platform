package ru.itmo.wmp.platform.application.notification;

import java.time.LocalDateTime;

public record RegistrationNotification(
    String email,
    String fullName,
    String isuNumber,
    String token,
    LocalDateTime expiresAt,
    String organizationName
) {
}
