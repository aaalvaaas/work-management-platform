package ru.itmo.wmp.platform.application.registration;

public record RegistrationSubmission(
    String organizationCode,
    String isuNumber,
    String lastName,
    String firstName,
    String patronymic,
    String email,
    String position,
    String password
) {
}
