package ru.itmo.wmp.platform.dto.auth;

public record TokenResponse(
    String accessToken,
    String tokenType,
    long expiresIn
) {
}
