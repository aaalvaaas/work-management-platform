package ru.itmo.wmp.platform.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import ru.itmo.wmp.platform.application.security.UserAuthorization;
import ru.itmo.wmp.platform.application.security.UserAuthorizationService;
import ru.itmo.wmp.platform.domain.organization.BlockRoleType;
import ru.itmo.wmp.platform.domain.organization.OrganizationRoleType;
import ru.itmo.wmp.platform.domain.user.GlobalRole;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Component("accessGuard")
@RequiredArgsConstructor
public class AccessGuard {
    private static final String ROLE_PREFIX = "ROLE_";

    private final UserAuthorizationService userAuthorizationService;

    public boolean isPlatformAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }

        String adminAuthority = ROLE_PREFIX + GlobalRole.PLATFORM_ADMIN.name();

        return authentication.getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(adminAuthority::equals);
    }

    public boolean isSelfOrPlatformAdmin(Long userId, Authentication authentication) {
        if (authentication == null || userId == null) {
            return false;
        }

        return isPlatformAdmin(authentication)
            || userId.toString().equals(authentication.getName());
    }

    public boolean isOrganizationManager(Long organizationId, Authentication authentication) {
        return hasOrganizationRole(
            organizationId,
            authentication,
            List.of(OrganizationRoleType.MANAGER)
        );
    }

    public boolean isOrganizationAdmin(Long organizationId, Authentication authentication) {
        return hasOrganizationRole(
            organizationId,
            authentication,
            List.of(OrganizationRoleType.MANAGER, OrganizationRoleType.ADMIN)
        );
    }

    public boolean isOrganizationMember(Long organizationId, Authentication authentication) {
        if (isPlatformAdmin(authentication)) {
            return true;
        }

        return authorizes(
            authentication,
            authorization -> authorization.isActiveMemberOf(organizationId)
        );
    }

    public boolean isBlockManager(Long blockId, Authentication authentication) {
        if (blockId == null) {
            return false;
        }

        return authorizes(
            authentication,
            authorization -> authorization.hasBlockRole(blockId, BlockRoleType.MANAGER)
        );
    }

    public boolean canManageBlock(
        Long organizationId,
        Long blockId,
        Authentication authentication
    ) {
        return isOrganizationAdmin(organizationId, authentication)
            || isBlockManager(blockId, authentication);
    }

    private boolean hasOrganizationRole(
        Long organizationId,
        Authentication authentication,
        List<OrganizationRoleType> roles
    ) {
        if (isPlatformAdmin(authentication)) {
            return true;
        }

        if (organizationId == null) {
            return false;
        }

        return authorizes(
            authentication,
            authorization -> authorization.hasOrganizationRole(organizationId, roles)
        );
    }

    private boolean authorizes(
        Authentication authentication,
        Predicate<UserAuthorization> predicate
    ) {
        return currentUserId(authentication)
            .map(userAuthorizationService::of)
            .filter(predicate)
            .isPresent();
    }

    private Optional<Long> currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(Long.valueOf(authentication.getName()));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }
}
