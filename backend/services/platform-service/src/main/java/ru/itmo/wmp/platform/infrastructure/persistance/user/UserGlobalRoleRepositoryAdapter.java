package ru.itmo.wmp.platform.infrastructure.persistance.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.itmo.wmp.platform.application.user.repository.UserGlobalRoleRepository;
import ru.itmo.wmp.platform.domain.user.GlobalRole;
import ru.itmo.wmp.platform.domain.user.UserGlobalRole;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserGlobalRoleRepositoryAdapter implements UserGlobalRoleRepository {
    private final JpaUserGlobalRoleRepository userGlobalRoleRepository;

    @Override
    public List<UserGlobalRole> findByUserId(Long userId) {
        return userGlobalRoleRepository.findByUserId(userId);
    }

    @Override
    public Optional<UserGlobalRole> findByUserIdAndRole(Long userId, GlobalRole role) {
        return userGlobalRoleRepository.findByUserIdAndRole(userId, role);
    }

    @Override
    public boolean existsByRole(GlobalRole role) {
        return userGlobalRoleRepository.existsByRole(role);
    }

    @Override
    public UserGlobalRole save(UserGlobalRole userGlobalRole) {
        return userGlobalRoleRepository.save(userGlobalRole);
    }

    @Override
    public void delete(UserGlobalRole userGlobalRole) {
        userGlobalRoleRepository.delete(userGlobalRole);
    }
}
