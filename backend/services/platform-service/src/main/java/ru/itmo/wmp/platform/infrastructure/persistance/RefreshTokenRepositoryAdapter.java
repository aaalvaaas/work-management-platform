package ru.itmo.wmp.platform.infrastructure.persistance;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.itmo.wmp.platform.application.repository.RefreshTokenRepository;
import ru.itmo.wmp.platform.domain.RefreshToken;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {
    private final JpaRefreshTokenRepository refreshTokenRepository;

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return refreshTokenRepository.findByTokenHash(tokenHash);
    }

    @Override
    public List<RefreshToken> findByUserIdAndRevokedAtIsNull(Long userId) {
        return refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId);
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public List<RefreshToken> saveAll(List<RefreshToken> refreshTokens) {
        return refreshTokenRepository.saveAll(refreshTokens);
    }
}
