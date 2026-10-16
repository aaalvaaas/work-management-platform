package ru.itmo.wmp.platform.dto.imports;

import java.util.List;

public record ImportPreviewResponse(
    ImportBatchResponse batch,
    List<ImportChangeResponse> changes
) {
}
