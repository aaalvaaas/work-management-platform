package ru.itmo.wmp.platform.application.nextcloud;

public interface NextcloudUserGateway {
    boolean exists(String uid);

    void create(NextcloudUser user, String password);

    void update(NextcloudUser user);

    void enable(String uid);

    void disable(String uid);
}
