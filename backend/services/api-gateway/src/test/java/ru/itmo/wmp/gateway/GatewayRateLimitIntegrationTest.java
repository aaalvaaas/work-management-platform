package ru.itmo.wmp.gateway;

import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.itmo.wmp.gateway.support.DownstreamStub;
import ru.itmo.wmp.gateway.support.TestAccessTokens;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(GatewayTestContainers.class)
public class GatewayRateLimitIntegrationTest {
    private static final RSAKey SIGNING_KEY = TestAccessTokens.signingKey();
    private static final DownstreamStub DOWNSTREAM = DownstreamStub.start(SIGNING_KEY);

    @Autowired
    private WebTestClient webTestClient;

    @DynamicPropertySource
    static void gatewayProperties(DynamicPropertyRegistry registry) {
        registry.add("wmp.services.platform-url", DOWNSTREAM::baseUrl);
        registry.add("wmp.services.mail-url", DOWNSTREAM::baseUrl);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", DOWNSTREAM::jwkSetUri);
        registry.add("wmp.rate-limit.auth.replenish-rate", () -> 1);
        registry.add("wmp.rate-limit.auth.burst-capacity", () -> 1);
        registry.add("wmp.rate-limit.api.replenish-rate", () -> 1);
        registry.add("wmp.rate-limit.api.burst-capacity", () -> 1);
    }

    @Test
    void shouldRejectSecondRequestOfTheSameUser() {
        String accessToken = TestAccessTokens.accessToken(SIGNING_KEY, "7001");

        webTestClient.get()
            .uri("/api/v1/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().exists("X-RateLimit-Remaining");

        webTestClient.get()
            .uri("/api/v1/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void shouldCountLimitPerUser() {
        webTestClient.get()
            .uri("/api/v1/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestAccessTokens.accessToken(SIGNING_KEY, "7002"))
            .exchange()
            .expectStatus().isOk();

        webTestClient.get()
            .uri("/api/v1/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestAccessTokens.accessToken(SIGNING_KEY, "7003"))
            .exchange()
            .expectStatus().isOk();
    }
}
