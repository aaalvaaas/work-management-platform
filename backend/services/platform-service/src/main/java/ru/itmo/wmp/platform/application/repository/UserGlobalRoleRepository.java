package ru.itmo.wmp.platform.application.repository;

import ru.itmo.wmp.platform.domain.GlobalRole;
import ru.itmo.wmp.platform.domain.UserGlobalRole;

import java.util.List;
import java.util.Optional;

public interface UserGlobalRoleRepository {
    List<UserGlobalRole> findByUserId(Long userId);

    Optional<UserGlobalRole> findByUserIdAndRole(Long userId, GlobalRole role);

    boolean existsByRole(GlobalRole role);

    UserGlobalRole save(UserGlobalRole userGlobalRole);

    void delete(UserGlobalRole userGlobalRole);
}
