package ru.itmo.wmp.platform.infrastructure.persistance.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.wmp.platform.domain.auth.RefreshToken;

import java.util.List;
import java.util.Optional;

public interface JpaRefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUserIdAndRevokedAtIsNull(Long userId);
}
