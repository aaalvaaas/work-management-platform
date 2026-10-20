package ru.itmo.wmp.tasks.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "platform")
public record PlatformProperties(
    String baseUrl,
    String internalKey,
    Duration timeout
) {
}
