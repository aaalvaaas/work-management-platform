package ru.itmo.wmp.platform.application.auth;

public record AuthTokens(
    String accessToken,
    String refreshToken,
    long accessTokenExpiresInSeconds
) {
}
