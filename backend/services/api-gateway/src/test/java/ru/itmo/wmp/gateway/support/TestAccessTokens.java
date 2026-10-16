package ru.itmo.wmp.gateway.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public final class TestAccessTokens {
    public static final String ISSUER = "wmp-platform-service";

    private TestAccessTokens() {
    }

    public static RSAKey signingKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");

            generator.initialize(2048);

            KeyPair keyPair = generator.generateKeyPair();

            return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyIDFromThumbprint()
                .build();
        } catch (NoSuchAlgorithmException | JOSEException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static String accessToken(RSAKey signingKey, String subject) {
        return accessToken(signingKey, subject, UUID.randomUUID().toString(), Instant.now());
    }

    public static String accessToken(
        RSAKey signingKey,
        String subject,
        String tokenId,
        Instant issuedAt
    ) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .id(tokenId)
            .issuer(ISSUER)
            .subject(subject)
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plus(Duration.ofMinutes(15)))
            .build();

        NimbusJwtEncoder encoder = new NimbusJwtEncoder(
            new ImmutableJWKSet<>(new JWKSet(signingKey))
        );

        return encoder
            .encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).build(),
                claims
            ))
            .getTokenValue();
    }
}
