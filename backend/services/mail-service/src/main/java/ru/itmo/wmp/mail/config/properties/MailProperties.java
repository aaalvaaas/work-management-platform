package ru.itmo.wmp.mail.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mail.imap")
public record MailProperties(
    String host,
    Integer port,
    String username,
    String password,
    String folder,
    Boolean ssl
) {
}
