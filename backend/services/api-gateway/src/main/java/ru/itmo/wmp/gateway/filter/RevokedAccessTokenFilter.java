package ru.itmo.wmp.gateway.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class RevokedAccessTokenFilter implements GlobalFilter, Ordered {
    private static final String DENIED_TOKEN_KEY = "wmp:access-token:denied:";
    private static final String USER_REVOKED_AT_KEY = "wmp:access-token:revoked-at:";

    private final ReactiveStringRedisTemplate redisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .filter(JwtAuthenticationToken.class::isInstance)
            .map(authentication -> ((JwtAuthenticationToken) authentication).getToken())
            .flatMap(this::isRevoked)
            .defaultIfEmpty(false)
            .flatMap(revoked -> revoked
                ? unauthorized(exchange)
                : chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private Mono<Boolean> isRevoked(Jwt accessToken) {
        return isTokenDenied(accessToken.getId())
            .flatMap(denied -> denied
                ? Mono.just(true)
                : isIssuedBeforeRevocation(accessToken));
    }

    private Mono<Boolean> isTokenDenied(String tokenId) {
        if (tokenId == null) {
            return Mono.just(false);
        }

        return redisTemplate.hasKey(DENIED_TOKEN_KEY + tokenId);
    }

    private Mono<Boolean> isIssuedBeforeRevocation(Jwt accessToken) {
        String subject = accessToken.getSubject();
        Instant issuedAt = accessToken.getIssuedAt();

        if (subject == null || issuedAt == null) {
            return Mono.just(false);
        }

        return redisTemplate.opsForValue()
            .get(USER_REVOKED_AT_KEY + subject)
            .map(revokedAt -> issuedAt.toEpochMilli() <= Long.parseLong(revokedAt))
            .defaultIfEmpty(false);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);

        return exchange.getResponse().setComplete();
    }
}
