package ru.itmo.wmp.platform.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "authorization")
public record AuthorizationProperties(
    Duration cacheTtl
) {
}
