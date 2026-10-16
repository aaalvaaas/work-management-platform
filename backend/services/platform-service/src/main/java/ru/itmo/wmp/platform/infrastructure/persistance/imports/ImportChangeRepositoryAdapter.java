package ru.itmo.wmp.platform.infrastructure.persistance.imports;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.itmo.wmp.platform.application.imports.repository.ImportChangeRepository;
import ru.itmo.wmp.platform.domain.imports.ImportChange;
import ru.itmo.wmp.platform.domain.imports.ImportChangeType;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ImportChangeRepositoryAdapter implements ImportChangeRepository {
    private final JpaImportChangeRepository importChangeRepository;

    @Override
    public List<ImportChange> findByBatchIdOrderByIdAsc(Long batchId) {
        return importChangeRepository.findByBatchIdOrderByIdAsc(batchId);
    }

    @Override
    public List<ImportChange> findByBatchIdAndType(Long batchId, ImportChangeType type) {
        return importChangeRepository.findByBatchIdAndType(batchId, type);
    }

    @Override
    public List<ImportChange> saveAll(List<ImportChange> changes) {
        return importChangeRepository.saveAll(changes);
    }
}
