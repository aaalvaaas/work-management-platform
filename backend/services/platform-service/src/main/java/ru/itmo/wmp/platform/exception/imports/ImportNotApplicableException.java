package ru.itmo.wmp.platform.exception.imports;

public class ImportNotApplicableException extends RuntimeException {
    public ImportNotApplicableException(Long batchId, String status) {
        super("Import batch id=" + batchId + " cannot be applied in status " + status);
    }
}
