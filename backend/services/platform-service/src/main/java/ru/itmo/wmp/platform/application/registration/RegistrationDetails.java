package ru.itmo.wmp.platform.application.registration;

import java.util.List;

public record RegistrationDetails(
    String isuNumber,
    String lastName,
    String firstName,
    String patronymic,
    String email,
    List<String> organizations
) {
}
