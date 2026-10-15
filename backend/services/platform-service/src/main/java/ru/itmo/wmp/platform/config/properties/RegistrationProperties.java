package ru.itmo.wmp.platform.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "registration")
public record RegistrationProperties(
    Duration emailConfirmationTtl,
    Duration invitationTtl
) {
}
