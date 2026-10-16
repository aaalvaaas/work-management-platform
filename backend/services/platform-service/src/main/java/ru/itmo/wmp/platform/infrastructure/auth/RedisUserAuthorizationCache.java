package ru.itmo.wmp.platform.infrastructure.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import ru.itmo.wmp.platform.application.security.UserAuthorization;
import ru.itmo.wmp.platform.application.security.UserAuthorizationCache;
import ru.itmo.wmp.platform.config.properties.AuthorizationProperties;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUserAuthorizationCache implements UserAuthorizationCache {
    private static final String SNAPSHOT_KEY = "wmp:authorization:user:";
    private static final String VERSION_KEY = "wmp:authorization:version:";
    private static final String INITIAL_VERSION = "0";
    private static final Duration VERSION_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AuthorizationProperties authorizationProperties;

    @Override
    public Optional<UserAuthorization> find(Long userId) {
        String snapshot = redisTemplate.opsForValue().get(snapshotKey(userId));

        if (snapshot == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(snapshot, UserAuthorization.class));
        } catch (JsonProcessingException exception) {
            log.warn("Unreadable authorization snapshot for userId={}", userId);

            return Optional.empty();
        }
    }

    @Override
    public void put(Long userId, UserAuthorization authorization) {
        try {
            redisTemplate.opsForValue().set(
                snapshotKey(userId),
                objectMapper.writeValueAsString(authorization),
                authorizationProperties.cacheTtl()
            );
        } catch (JsonProcessingException exception) {
            log.warn("Unable to cache authorization for userId={}", userId);
        }
    }

    @Override
    public void evict(Long userId) {
        String versionKey = VERSION_KEY + userId;

        redisTemplate.opsForValue().increment(versionKey);
        redisTemplate.expire(versionKey, VERSION_TTL);
    }

    private String snapshotKey(Long userId) {
        return SNAPSHOT_KEY + userId + ":" + version(userId);
    }

    private String version(Long userId) {
        String version = redisTemplate.opsForValue().get(VERSION_KEY + userId);

        return version == null ? INITIAL_VERSION : version;
    }
}
