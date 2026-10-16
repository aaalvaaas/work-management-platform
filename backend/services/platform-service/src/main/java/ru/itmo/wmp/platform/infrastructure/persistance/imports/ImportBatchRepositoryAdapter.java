package ru.itmo.wmp.platform.infrastructure.persistance.imports;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.itmo.wmp.platform.application.imports.repository.ImportBatchRepository;
import ru.itmo.wmp.platform.domain.imports.ImportBatch;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ImportBatchRepositoryAdapter implements ImportBatchRepository {
    private final JpaImportBatchRepository importBatchRepository;

    @Override
    public List<ImportBatch> findByOrganizationIdOrderByIdDesc(Long organizationId) {
        return importBatchRepository.findByOrganizationIdOrderByIdDesc(organizationId);
    }

    @Override
    public Optional<ImportBatch> findById(Long id) {
        return importBatchRepository.findById(id);
    }

    @Override
    public ImportBatch save(ImportBatch batch) {
        return importBatchRepository.save(batch);
    }
}
