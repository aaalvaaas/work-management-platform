package ru.itmo.wmp.gateway;

import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.itmo.wmp.gateway.support.DownstreamStub;
import ru.itmo.wmp.gateway.support.TestAccessTokens;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(GatewayTestContainers.class)
public class GatewayRoutingIntegrationTest {
    private static final RSAKey SIGNING_KEY = TestAccessTokens.signingKey();
    private static final DownstreamStub DOWNSTREAM = DownstreamStub.start(SIGNING_KEY);

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    @DynamicPropertySource
    static void gatewayProperties(DynamicPropertyRegistry registry) {
        registry.add("wmp.services.platform-url", DOWNSTREAM::baseUrl);
        registry.add("wmp.services.mail-url", DOWNSTREAM::baseUrl);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", DOWNSTREAM::jwkSetUri);
        registry.add("wmp.rate-limit.auth.replenish-rate", () -> 1000);
        registry.add("wmp.rate-limit.auth.burst-capacity", () -> 1000);
        registry.add("wmp.rate-limit.api.replenish-rate", () -> 1000);
        registry.add("wmp.rate-limit.api.burst-capacity", () -> 1000);
    }

    @Test
    void shouldRouteLoginWithoutAuthentication() {
        webTestClient.post()
            .uri("/api/v1/auth/login")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.stub").isEqualTo(true);
    }

    @Test
    void shouldRejectProtectedRouteWithoutToken() {
        webTestClient.get()
            .uri("/api/v1/users/me")
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    void shouldRouteProtectedRequestAndForwardTheToken() {
        String accessToken = TestAccessTokens.accessToken(SIGNING_KEY, "42");

        webTestClient.get()
            .uri("/api/v1/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .exchange()
            .expectStatus().isOk();

        assertThat(DOWNSTREAM.receivedAuthorizationHeaders())
            .contains("Bearer " + accessToken);
    }

    @Test
    void shouldRejectTokenSignedByAnotherKey() {
        String accessToken = TestAccessTokens.accessToken(TestAccessTokens.signingKey(), "42");

        webTestClient.get()
            .uri("/api/v1/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    void shouldRejectTokenListedInTheDenyList() {
        String tokenId = UUID.randomUUID().toString();

        String accessToken = TestAccessTokens.accessToken(
            SIGNING_KEY,
            "43",
            tokenId,
            Instant.now()
        );

        redisTemplate.opsForValue()
            .set("wmp:access-token:denied:" + tokenId, "1", Duration.ofMinutes(5))
            .block();

        webTestClient.get()
            .uri("/api/v1/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    void shouldRejectTokenIssuedBeforeUserRevocation() {
        String accessToken = TestAccessTokens.accessToken(
            SIGNING_KEY,
            "44",
            UUID.randomUUID().toString(),
            Instant.now().minusSeconds(60)
        );

        redisTemplate.opsForValue()
            .set(
                "wmp:access-token:revoked-at:44",
                String.valueOf(Instant.now().toEpochMilli()),
                Duration.ofMinutes(5)
            )
            .block();

        webTestClient.get()
            .uri("/api/v1/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    void shouldRouteMailRequests() {
        webTestClient.get()
            .uri("/api/v1/mails")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestAccessTokens.accessToken(SIGNING_KEY, "45"))
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void shouldAnswerCorsPreflightForAllowedOrigin() {
        webTestClient.options()
            .uri("/api/v1/users/me")
            .header(HttpHeaders.ORIGIN, "http://localhost:5173")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().valueEquals(
                HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                "http://localhost:5173"
            );
    }

    @Test
    void shouldRejectUnknownPath() {
        webTestClient.get()
            .uri("/api/v1/unknown")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestAccessTokens.accessToken(SIGNING_KEY, "46"))
            .exchange()
            .expectStatus().isNotFound();
    }
}
