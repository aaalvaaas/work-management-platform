package ru.itmo.wmp.platform.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import ru.itmo.wmp.platform.config.properties.JwtProperties;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class JwtConfig {
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Bean
    public JwtEncoder jwtEncoder(JwtProperties jwtProperties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(jwtProperties)));
    }

    @Bean
    public JwtDecoder jwtDecoder(JwtProperties jwtProperties) {
        return NimbusJwtDecoder
            .withSecretKey(secretKey(jwtProperties))
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    }

    private SecretKeySpec secretKey(JwtProperties jwtProperties) {
        return new SecretKeySpec(
            jwtProperties.secret().getBytes(StandardCharsets.UTF_8),
            HMAC_ALGORITHM
        );
    }
}
