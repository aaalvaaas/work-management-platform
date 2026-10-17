package ru.itmo.wmp.tasks.config.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;

public final class JwtPrincipal {
    private static final String PLATFORM_ADMIN = "ROLE_PLATFORM_ADMIN";

    private JwtPrincipal() {
    }

    public static Long userId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new IllegalStateException("Request is not authenticated");
        }

        return Long.valueOf(jwt.getSubject());
    }

    public static boolean isPlatformAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(PLATFORM_ADMIN::equals);
    }

    public static Optional<String> currentToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken token) {
            return Optional.of(token.getToken().getTokenValue());
        }

        return Optional.empty();
    }
}
