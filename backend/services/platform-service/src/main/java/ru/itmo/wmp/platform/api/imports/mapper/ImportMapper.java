package ru.itmo.wmp.platform.api.imports.mapper;

import org.springframework.stereotype.Component;
import ru.itmo.wmp.platform.application.imports.ImportSummary;
import ru.itmo.wmp.platform.domain.imports.ImportBatch;
import ru.itmo.wmp.platform.domain.imports.ImportChange;
import ru.itmo.wmp.platform.dto.imports.ImportBatchResponse;
import ru.itmo.wmp.platform.dto.imports.ImportChangeResponse;
import ru.itmo.wmp.platform.dto.imports.ImportPreviewResponse;

import java.util.List;

@Component
public class ImportMapper {
    public ImportBatchResponse toResponse(ImportBatch batch, ImportSummary summary) {
        return new ImportBatchResponse(
            batch.getId(),
            batch.getOrganization().getId(),
            batch.getFilename(),
            batch.getStatus(),
            batch.getTotalRows(),
            summary.created(),
            summary.updated(),
            summary.deactivated(),
            summary.unchanged(),
            summary.errors(),
            batch.getCreatedAt(),
            batch.getAppliedAt()
        );
    }

    public ImportChangeResponse toResponse(ImportChange change) {
        return new ImportChangeResponse(
            change.getRowNumber(),
            change.getType(),
            change.getIsuNumber(),
            change.getLastName(),
            change.getFirstName(),
            change.getPatronymic(),
            change.getEmail(),
            change.getPosition(),
            change.getBlockNames(),
            change.getDetails()
        );
    }

    public ImportPreviewResponse toPreview(
        ImportBatch batch,
        ImportSummary summary,
        List<ImportChange> changes
    ) {
        return new ImportPreviewResponse(
            toResponse(batch, summary),
            changes.stream().map(this::toResponse).toList()
        );
    }
}
