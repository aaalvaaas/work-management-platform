package ru.itmo.wmp.platform.infrastructure.persistance.user;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.wmp.platform.domain.user.GlobalRole;
import ru.itmo.wmp.platform.domain.user.UserGlobalRole;

import java.util.List;
import java.util.Optional;

public interface JpaUserGlobalRoleRepository extends JpaRepository<UserGlobalRole, Long> {
    List<UserGlobalRole> findByUserId(Long userId);

    Optional<UserGlobalRole> findByUserIdAndRole(Long userId, GlobalRole role);

    boolean existsByRole(GlobalRole role);
}
