package ru.itmo.wmp.platform.application.auth;

import java.time.Instant;

public interface AccessTokenDenyList {
    void deny(String tokenId, Instant expiresAt);

    void denyAllOf(String userId);

    boolean isDenied(String tokenId, String userId, Instant issuedAt);
}
