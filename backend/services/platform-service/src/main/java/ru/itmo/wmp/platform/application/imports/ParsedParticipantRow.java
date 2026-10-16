package ru.itmo.wmp.platform.application.imports;

import java.util.List;

public record ParsedParticipantRow(
    int rowNumber,
    String isuNumber,
    String lastName,
    String firstName,
    String patronymic,
    String email,
    String position,
    List<String> blockNames
) {
}
