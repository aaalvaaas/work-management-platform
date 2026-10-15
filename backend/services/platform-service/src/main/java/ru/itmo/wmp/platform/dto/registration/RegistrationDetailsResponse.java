package ru.itmo.wmp.platform.dto.registration;

import java.util.List;

public record RegistrationDetailsResponse(
    String isuNumber,
    String lastName,
    String firstName,
    String patronymic,
    String email,
    List<String> organizations
) {
}
