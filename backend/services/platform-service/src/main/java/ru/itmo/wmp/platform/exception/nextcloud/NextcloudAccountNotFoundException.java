package ru.itmo.wmp.platform.exception.nextcloud;

public class NextcloudAccountNotFoundException extends RuntimeException {
    public NextcloudAccountNotFoundException(Long userId) {
        super("Nextcloud account for user id=" + userId + " not found");
    }
}
