package ru.itmo.wmp.platform.config.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import ru.itmo.wmp.platform.domain.GlobalRole;

@Component("accessGuard")
public class AccessGuard {
    private static final String ROLE_PREFIX = "ROLE_";

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
}
