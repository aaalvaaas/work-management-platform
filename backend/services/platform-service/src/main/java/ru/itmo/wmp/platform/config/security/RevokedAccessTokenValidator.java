package ru.itmo.wmp.platform.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import ru.itmo.wmp.platform.application.auth.AccessTokenDenyList;

@Component
@RequiredArgsConstructor
public class RevokedAccessTokenValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error REVOKED = new OAuth2Error(
        "invalid_token",
        "The access token has been revoked",
        null
    );

    private final AccessTokenDenyList accessTokenDenyList;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        boolean denied = accessTokenDenyList.isDenied(
            token.getId(),
            token.getSubject(),
            token.getIssuedAt()
        );

        return denied
            ? OAuth2TokenValidatorResult.failure(REVOKED)
            : OAuth2TokenValidatorResult.success();
    }
}
