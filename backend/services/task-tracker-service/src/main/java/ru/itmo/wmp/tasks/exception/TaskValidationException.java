package ru.itmo.wmp.tasks.exception;

public class TaskValidationException extends RuntimeException {
    public TaskValidationException(String message) {
        super(message);
    }
}
