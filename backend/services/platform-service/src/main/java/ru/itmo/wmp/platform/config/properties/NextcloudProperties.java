package ru.itmo.wmp.platform.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nextcloud")
public record NextcloudProperties(
    boolean enabled,
    String baseUrl,
    String adminUser,
    String adminPassword
) {
}
