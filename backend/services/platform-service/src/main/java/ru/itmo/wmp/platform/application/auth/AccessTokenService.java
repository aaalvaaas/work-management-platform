package ru.itmo.wmp.platform.application.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import ru.itmo.wmp.platform.config.properties.JwtProperties;
import ru.itmo.wmp.platform.domain.user.GlobalRole;
import ru.itmo.wmp.platform.domain.user.User;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AccessTokenService {
    public static final String ROLES_CLAIM = "roles";
    public static final String ISU_NUMBER_CLAIM = "isu";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public String issue(User user, Set<GlobalRole> globalRoles) {
        Instant issuedAt = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(jwtProperties.issuer())
            .subject(String.valueOf(user.getId()))
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plus(jwtProperties.accessTokenTtl()))
            .claim(ISU_NUMBER_CLAIM, user.getIsuNumber())
            .claim(ROLES_CLAIM, globalRoles.stream().map(Enum::name).toList())
            .build();

        JwsHeader header = JwsHeader
            .with(SignatureAlgorithm.RS256)
            .build();

        return jwtEncoder
            .encode(JwtEncoderParameters.from(header, claims))
            .getTokenValue();
    }

    public Duration accessTokenTtl() {
        return jwtProperties.accessTokenTtl();
    }
}
