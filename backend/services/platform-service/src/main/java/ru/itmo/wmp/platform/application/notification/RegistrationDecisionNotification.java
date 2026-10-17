package ru.itmo.wmp.platform.application.notification;

public record RegistrationDecisionNotification(
    String email,
    String fullName,
    String isuNumber,
    String organizationName,
    boolean approved,
    String comment
) {
}
