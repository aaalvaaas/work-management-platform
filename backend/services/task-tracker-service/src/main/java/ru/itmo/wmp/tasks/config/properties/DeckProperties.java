package ru.itmo.wmp.tasks.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "deck")
public record DeckProperties(
    boolean enabled,
    String baseUrl,
    String adminUser,
    String adminPassword,
    Duration timeout,
    Duration pushInterval,
    Duration pullInterval
) {
}
