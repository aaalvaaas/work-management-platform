package ru.itmo.wmp.tasks.exception;

public class TaskTransitionException extends RuntimeException {
    public TaskTransitionException(String message) {
        super(message);
    }
}
