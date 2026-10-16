package ru.itmo.wmp.platform.infrastructure.persistance.imports;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.wmp.platform.domain.imports.ImportBatch;

import java.util.List;

public interface JpaImportBatchRepository extends JpaRepository<ImportBatch, Long> {
    List<ImportBatch> findByOrganizationIdOrderByIdDesc(Long organizationId);
}
