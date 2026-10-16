package ru.itmo.wmp.platform.exception.imports;

public class ImportBatchNotFoundException extends RuntimeException {
    public ImportBatchNotFoundException(Long organizationId, Long batchId) {
        super("Import batch id=" + batchId + " not found in organization id=" + organizationId);
    }
}
