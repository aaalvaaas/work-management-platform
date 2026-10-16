package ru.itmo.wmp.platform.infrastructure.persistance.imports;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.wmp.platform.domain.imports.ImportChange;
import ru.itmo.wmp.platform.domain.imports.ImportChangeType;

import java.util.List;

public interface JpaImportChangeRepository extends JpaRepository<ImportChange, Long> {
    List<ImportChange> findByBatchIdOrderByIdAsc(Long batchId);

    List<ImportChange> findByBatchIdAndType(Long batchId, ImportChangeType type);
}
