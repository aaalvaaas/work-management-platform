package ru.itmo.wmp.platform.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.util.StringUtils;
import ru.itmo.wmp.platform.config.properties.JwtProperties;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Slf4j
@Configuration
public class JwtConfig {
    private static final String KEY_ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;

    @Bean
    public RSAKey jwtSigningKey(JwtProperties jwtProperties, ResourceLoader resourceLoader) {
        if (hasConfiguredKeyPair(jwtProperties)) {
            return signingKey(
                readPublicKey(jwtProperties.publicKeyLocation(), resourceLoader),
                readPrivateKey(jwtProperties.privateKeyLocation(), resourceLoader),
                jwtProperties.keyId()
            );
        }

        log.warn(
            "No JWT key pair configured, generating an ephemeral one: "
                + "issued access tokens stop being valid after a restart"
        );

        KeyPair keyPair = generateKeyPair();

        return signingKey(
            (RSAPublicKey) keyPair.getPublic(),
            (RSAPrivateKey) keyPair.getPrivate(),
            jwtProperties.keyId()
        );
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(RSAKey jwtSigningKey) {
        return new ImmutableJWKSet<>(new JWKSet(jwtSigningKey));
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAKey jwtSigningKey, JwtProperties jwtProperties) {
        NimbusJwtDecoder decoder;

        try {
            decoder = NimbusJwtDecoder
                .withPublicKey(jwtSigningKey.toRSAPublicKey())
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        } catch (JOSEException exception) {
            throw new IllegalStateException("Unable to read the JWT public key", exception);
        }

        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(jwtProperties.issuer()));

        return decoder;
    }

    private boolean hasConfiguredKeyPair(JwtProperties jwtProperties) {
        return StringUtils.hasText(jwtProperties.privateKeyLocation())
            && StringUtils.hasText(jwtProperties.publicKeyLocation());
    }

    private RSAKey signingKey(RSAPublicKey publicKey, RSAPrivateKey privateKey, String keyId) {
        RSAKey.Builder builder = new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyUse(KeyUse.SIGNATURE)
            .algorithm(JWSAlgorithm.RS256);

        if (StringUtils.hasText(keyId)) {
            return builder.keyID(keyId).build();
        }

        try {
            return builder.keyIDFromThumbprint().build();
        } catch (JOSEException exception) {
            throw new IllegalStateException("Unable to derive a key identifier", exception);
        }
    }

    private RSAPublicKey readPublicKey(String location, ResourceLoader resourceLoader) {
        try (InputStream content = open(location, resourceLoader)) {
            return RsaKeyConverters.x509().convert(content);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read the JWT public key: " + location, exception);
        }
    }

    private RSAPrivateKey readPrivateKey(String location, ResourceLoader resourceLoader) {
        try (InputStream content = open(location, resourceLoader)) {
            return RsaKeyConverters.pkcs8().convert(content);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read the JWT private key: " + location, exception);
        }
    }

    private InputStream open(String location, ResourceLoader resourceLoader) throws IOException {
        Resource resource = resourceLoader.getResource(location);

        if (!resource.exists()) {
            throw new IllegalStateException("JWT key not found: " + location);
        }

        return resource.getInputStream();
    }

    private KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM);

            generator.initialize(KEY_SIZE);

            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("RSA is not available", exception);
        }
    }
}
