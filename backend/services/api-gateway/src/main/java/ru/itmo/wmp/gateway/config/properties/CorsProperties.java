package ru.itmo.wmp.gateway.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "wmp.cors")
public record CorsProperties(
    List<String> allowedOrigins
) {
}
