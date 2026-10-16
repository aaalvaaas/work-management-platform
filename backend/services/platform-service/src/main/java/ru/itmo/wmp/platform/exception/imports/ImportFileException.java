package ru.itmo.wmp.platform.exception.imports;

public class ImportFileException extends RuntimeException {
    public ImportFileException(String message) {
        super(message);
    }

    public ImportFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
