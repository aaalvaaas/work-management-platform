package ru.itmo.wmp.platform.application.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.wmp.platform.application.auth.RefreshTokenService;
import ru.itmo.wmp.platform.application.nextcloud.NextcloudAccountService;
import ru.itmo.wmp.platform.application.user.repository.UserGlobalRoleRepository;
import ru.itmo.wmp.platform.application.user.repository.UserRepository;
import ru.itmo.wmp.platform.domain.user.GlobalRole;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.domain.user.UserGlobalRole;
import ru.itmo.wmp.platform.domain.user.UserStatus;
import ru.itmo.wmp.platform.exception.user.SelfManagementNotAllowedException;

@Service
@RequiredArgsConstructor
@Transactional
public class UserAdministrationService {
    private final UserRepository userRepository;
    private final UserGlobalRoleRepository userGlobalRoleRepository;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private final NextcloudAccountService nextcloudAccountService;

    public User block(Long userId, Long actorUserId) {
        if (userId.equals(actorUserId)) {
            throw new SelfManagementNotAllowedException("block");
        }

        User user = userService.getById(userId);

        user.setStatus(UserStatus.BLOCKED);

        User blocked = userRepository.save(user);

        refreshTokenService.revokeAllOf(userId);

        nextcloudAccountService.disable(blocked);

        return blocked;
    }

    public User unblock(Long userId) {
        User user = userService.getById(userId);

        user.setStatus(UserStatus.ACTIVE);

        User unblocked = userRepository.save(user);

        nextcloudAccountService.enable(unblocked);

        return unblocked;
    }

    public User grantRole(Long userId, GlobalRole role, Long actorUserId) {
        User user = userService.getById(userId);

        if (userGlobalRoleRepository.findByUserIdAndRole(userId, role).isEmpty()) {
            UserGlobalRole userGlobalRole = new UserGlobalRole();

            userGlobalRole.setUser(user);
            userGlobalRole.setRole(role);
            userGlobalRole.setGrantedBy(userService.getById(actorUserId));

            userGlobalRoleRepository.save(userGlobalRole);
        }

        return user;
    }

    public User revokeRole(Long userId, GlobalRole role, Long actorUserId) {
        if (userId.equals(actorUserId) && role == GlobalRole.PLATFORM_ADMIN) {
            throw new SelfManagementNotAllowedException("revoke PLATFORM_ADMIN");
        }

        User user = userService.getById(userId);

        userGlobalRoleRepository.findByUserIdAndRole(userId, role)
            .ifPresent(userGlobalRoleRepository::delete);

        return user;
    }
}
