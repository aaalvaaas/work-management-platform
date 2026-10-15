package ru.itmo.wmp.platform.infrastructure.persistance;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.wmp.platform.domain.GlobalRole;
import ru.itmo.wmp.platform.domain.UserGlobalRole;

import java.util.List;
import java.util.Optional;

public interface JpaUserGlobalRoleRepository extends JpaRepository<UserGlobalRole, Long> {
    List<UserGlobalRole> findByUserId(Long userId);

    Optional<UserGlobalRole> findByUserIdAndRole(Long userId, GlobalRole role);

    boolean existsByRole(GlobalRole role);
}
