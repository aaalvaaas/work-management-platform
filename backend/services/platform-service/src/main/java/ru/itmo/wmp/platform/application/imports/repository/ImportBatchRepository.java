package ru.itmo.wmp.platform.application.imports.repository;

import ru.itmo.wmp.platform.domain.imports.ImportBatch;

import java.util.List;
import java.util.Optional;

public interface ImportBatchRepository {
    List<ImportBatch> findByOrganizationIdOrderByIdDesc(Long organizationId);

    Optional<ImportBatch> findById(Long id);

    ImportBatch save(ImportBatch batch);
}
