package ru.itmo.wmp.tasks.exception;

public class PlatformUnavailableException extends RuntimeException {
    public PlatformUnavailableException(String path, Throwable cause) {
        super("Platform service is unavailable: " + path, cause);
    }
}
