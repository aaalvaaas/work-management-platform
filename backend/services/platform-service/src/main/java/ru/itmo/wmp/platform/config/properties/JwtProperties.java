package ru.itmo.wmp.platform.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
    String issuer,
    Duration accessTokenTtl,
    Duration refreshTokenTtl,
    String keyId,
    String privateKeyLocation,
    String publicKeyLocation
) {
}
