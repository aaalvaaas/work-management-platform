package ru.itmo.wmp.platform.api.auth;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class JwkSetController {
    private final RSAKey jwtSigningKey;

    @GetMapping("/.well-known/jwks.json")
    @Operation(
        summary = "Public signing keys",
        description = "Returns the public part of the keys used to sign access tokens"
    )
    public Map<String, Object> jwkSet() {
        return new JWKSet(jwtSigningKey)
            .toPublicJWKSet()
            .toJSONObject();
    }
}
