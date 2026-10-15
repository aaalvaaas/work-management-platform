package ru.itmo.wmp.platform.application.nextcloud;

public record NextcloudUser(
    String uid,
    String displayName,
    String email
) {
}
