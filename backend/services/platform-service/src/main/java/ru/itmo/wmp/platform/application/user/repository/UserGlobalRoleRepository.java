package ru.itmo.wmp.platform.application.user.repository;

import ru.itmo.wmp.platform.domain.user.GlobalRole;
import ru.itmo.wmp.platform.domain.user.UserGlobalRole;

import java.util.List;
import java.util.Optional;

public interface UserGlobalRoleRepository {
    List<UserGlobalRole> findByUserId(Long userId);

    Optional<UserGlobalRole> findByUserIdAndRole(Long userId, GlobalRole role);

    boolean existsByRole(GlobalRole role);

    UserGlobalRole save(UserGlobalRole userGlobalRole);

    void delete(UserGlobalRole userGlobalRole);
}
