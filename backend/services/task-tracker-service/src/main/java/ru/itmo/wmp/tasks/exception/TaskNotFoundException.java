package ru.itmo.wmp.tasks.exception;

public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(Long taskId) {
        super("Task id=" + taskId + " not found");
    }
}
