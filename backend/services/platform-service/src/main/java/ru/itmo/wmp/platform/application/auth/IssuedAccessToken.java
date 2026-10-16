package ru.itmo.wmp.platform.application.auth;

import java.time.Instant;

public record IssuedAccessToken(
    String tokenId,
    Instant expiresAt
) {
}
