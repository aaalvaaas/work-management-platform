package ru.itmo.wmp.tasks.exception;

public class TaskAccessDeniedException extends RuntimeException {
    public TaskAccessDeniedException(String message) {
        super(message);
    }
}
