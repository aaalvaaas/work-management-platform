package ru.itmo.wmp.platform.exception.nextcloud;

public class NextcloudException extends RuntimeException {
    public NextcloudException(String message) {
        super(message);
    }

    public NextcloudException(String message, Throwable cause) {
        super(message, cause);
    }
}
