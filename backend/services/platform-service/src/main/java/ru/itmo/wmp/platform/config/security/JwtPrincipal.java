package ru.itmo.wmp.platform.config.security;

import org.springframework.security.oauth2.jwt.Jwt;

public final class JwtPrincipal {
    private JwtPrincipal() {
    }

    public static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
