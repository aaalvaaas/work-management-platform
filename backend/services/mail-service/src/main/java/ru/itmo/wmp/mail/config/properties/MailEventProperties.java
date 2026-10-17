package ru.itmo.wmp.mail.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wmp.events")
public record MailEventProperties(
    String mailParsedTopic,
    int partitions
) {
}
