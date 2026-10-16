package ru.itmo.wmp.platform.infrastructure.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import ru.itmo.wmp.platform.application.auth.AccessTokenDenyList;
import ru.itmo.wmp.platform.config.properties.JwtProperties;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class RedisAccessTokenDenyList implements AccessTokenDenyList {
    private static final String DENIED_TOKEN_KEY = "wmp:access-token:denied:";
    private static final String USER_REVOKED_AT_KEY = "wmp:access-token:revoked-at:";
    private static final String MARKER = "1";

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;

    @Override
    public void deny(String tokenId, Instant expiresAt) {
        if (tokenId == null || expiresAt == null) {
            return;
        }

        Duration remaining = Duration.between(Instant.now(), expiresAt);

        if (remaining.isZero() || remaining.isNegative()) {
            return;
        }

        redisTemplate.opsForValue().set(DENIED_TOKEN_KEY + tokenId, MARKER, remaining);
    }

    @Override
    public void denyAllOf(String userId) {
        if (userId == null) {
            return;
        }

        redisTemplate.opsForValue().set(
            USER_REVOKED_AT_KEY + userId,
            String.valueOf(Instant.now().toEpochMilli()),
            jwtProperties.accessTokenTtl()
        );
    }

    @Override
    public boolean isDenied(String tokenId, String userId, Instant issuedAt) {
        if (tokenId != null && Boolean.TRUE.equals(redisTemplate.hasKey(DENIED_TOKEN_KEY + tokenId))) {
            return true;
        }

        if (userId == null || issuedAt == null) {
            return false;
        }

        String revokedAt = redisTemplate.opsForValue().get(USER_REVOKED_AT_KEY + userId);

        return revokedAt != null && issuedAt.toEpochMilli() <= Long.parseLong(revokedAt);
    }
}
