package ru.itmo.wmp.platform.api.auth.mapper;

import org.springframework.stereotype.Component;
import ru.itmo.wmp.platform.application.auth.AuthTokens;
import ru.itmo.wmp.platform.dto.auth.TokenResponse;

@Component
public class TokenMapper {
    private static final String TOKEN_TYPE = "Bearer";

    public TokenResponse toResponse(AuthTokens tokens) {
        return new TokenResponse(
            tokens.accessToken(),
            tokens.refreshToken(),
            TOKEN_TYPE,
            tokens.accessTokenExpiresInSeconds()
        );
    }
}
