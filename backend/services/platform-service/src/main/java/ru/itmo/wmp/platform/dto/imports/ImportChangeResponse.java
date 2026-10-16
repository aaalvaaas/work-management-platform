package ru.itmo.wmp.platform.dto.imports;

import ru.itmo.wmp.platform.domain.imports.ImportChangeType;

public record ImportChangeResponse(
    Integer rowNumber,
    ImportChangeType type,
    String isuNumber,
    String lastName,
    String firstName,
    String patronymic,
    String email,
    String position,
    String blockNames,
    String details
) {
}
