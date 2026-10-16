package ru.itmo.wmp.platform.application.security;

import java.util.Optional;

public interface UserAuthorizationCache {
    Optional<UserAuthorization> find(Long userId);

    void put(Long userId, UserAuthorization authorization);

    void evict(Long userId);
}
