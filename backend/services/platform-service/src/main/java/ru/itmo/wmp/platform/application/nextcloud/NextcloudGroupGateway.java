package ru.itmo.wmp.platform.application.nextcloud;

import java.util.List;

public interface NextcloudGroupGateway {
    void ensureGroup(String groupId);

    List<String> groupsOf(String uid);

    void addUserToGroup(String uid, String groupId);

    void removeUserFromGroup(String uid, String groupId);
}
