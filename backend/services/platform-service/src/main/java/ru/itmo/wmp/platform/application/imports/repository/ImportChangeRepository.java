package ru.itmo.wmp.platform.application.imports.repository;

import ru.itmo.wmp.platform.domain.imports.ImportChange;
import ru.itmo.wmp.platform.domain.imports.ImportChangeType;

import java.util.List;

public interface ImportChangeRepository {
    List<ImportChange> findByBatchIdOrderByIdAsc(Long batchId);

    List<ImportChange> findByBatchIdAndType(Long batchId, ImportChangeType type);

    List<ImportChange> saveAll(List<ImportChange> changes);
}
