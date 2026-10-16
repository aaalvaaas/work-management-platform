package ru.itmo.wmp.platform.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.refresh-cookie")
public record RefreshCookieProperties(
    String name,
    String path,
    boolean secure,
    String sameSite
) {
}
