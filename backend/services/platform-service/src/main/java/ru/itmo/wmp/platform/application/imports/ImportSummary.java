package ru.itmo.wmp.platform.application.imports;

public record ImportSummary(
    int created,
    int updated,
    int deactivated,
    int unchanged,
    int errors
) {
}
