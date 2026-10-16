package ru.itmo.wmp.platform.dto.imports;

import ru.itmo.wmp.platform.domain.imports.ImportBatchStatus;

import java.time.LocalDateTime;

public record ImportBatchResponse(
    Long id,
    Long organizationId,
    String filename,
    ImportBatchStatus status,
    int totalRows,
    int created,
    int updated,
    int deactivated,
    int unchanged,
    int errors,
    LocalDateTime createdAt,
    LocalDateTime appliedAt
) {
}
