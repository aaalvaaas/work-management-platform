package ru.itmo.wmp.platform.dto.response;

public record TokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn
) {
}
