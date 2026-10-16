package ru.itmo.wmp.platform.api.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import ru.itmo.wmp.platform.config.properties.JwtProperties;
import ru.itmo.wmp.platform.config.properties.RefreshCookieProperties;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookie {
    private final RefreshCookieProperties cookieProperties;
    private final JwtProperties jwtProperties;

    public ResponseCookie issue(String refreshToken) {
        return cookie(refreshToken, jwtProperties.refreshTokenTtl());
    }

    public ResponseCookie clear() {
        return cookie("", Duration.ZERO);
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(cookieProperties.name(), value)
            .httpOnly(true)
            .secure(cookieProperties.secure())
            .sameSite(cookieProperties.sameSite())
            .path(cookieProperties.path())
            .maxAge(maxAge)
            .build();
    }
}
