package ru.itmo.wmp.platform.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bootstrap.admin")
public record BootstrapAdminProperties(
    String isuNumber,
    String email,
    String lastName,
    String firstName,
    String password
) {
    public boolean isComplete() {
        return isNotBlank(isuNumber)
            && isNotBlank(email)
            && isNotBlank(lastName)
            && isNotBlank(firstName)
            && isNotBlank(password);
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
