package ru.itmo.wmp.platform.application.nextcloud;

public interface NextcloudGroupGateway {
    void ensureGroup(String groupId);

    void addUserToGroup(String uid, String groupId);

    void removeUserFromGroup(String uid, String groupId);
}
