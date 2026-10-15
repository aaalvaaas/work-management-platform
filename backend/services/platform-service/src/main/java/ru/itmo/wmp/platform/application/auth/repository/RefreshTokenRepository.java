package ru.itmo.wmp.platform.application.auth.repository;

import ru.itmo.wmp.platform.domain.auth.RefreshToken;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUserIdAndRevokedAtIsNull(Long userId);

    RefreshToken save(RefreshToken refreshToken);

    List<RefreshToken> saveAll(List<RefreshToken> refreshTokens);
}
