package ru.itmo.wmp.platform.api.auth.mapper;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import ru.itmo.wmp.platform.application.auth.AuthTokens;
import ru.itmo.wmp.platform.application.auth.IssuedAccessToken;
import ru.itmo.wmp.platform.dto.auth.TokenResponse;

@Component
public class TokenMapper {
    private static final String TOKEN_TYPE = "Bearer";

    public TokenResponse toResponse(AuthTokens tokens) {
        return new TokenResponse(
            tokens.accessToken(),
            TOKEN_TYPE,
            tokens.accessTokenExpiresInSeconds()
        );
    }

    public IssuedAccessToken toIssuedAccessToken(Jwt accessToken) {
        if (accessToken == null) {
            return null;
        }

        return new IssuedAccessToken(accessToken.getId(), accessToken.getExpiresAt());
    }
}
