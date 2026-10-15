package ru.itmo.wmp.platform.application.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.wmp.platform.application.auth.repository.RefreshTokenRepository;
import ru.itmo.wmp.platform.application.security.SecureTokenService;
import ru.itmo.wmp.platform.config.properties.JwtProperties;
import ru.itmo.wmp.platform.domain.auth.RefreshToken;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.exception.auth.InvalidTokenException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureTokenService secureTokenService;
    private final JwtProperties jwtProperties;

    public String issue(User user) {
        String token = secureTokenService.generate();

        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setUser(user);
        refreshToken.setTokenHash(secureTokenService.hash(token));
        refreshToken.setExpiresAt(
            LocalDateTime.now().plus(jwtProperties.refreshTokenTtl())
        );

        refreshTokenRepository.save(refreshToken);

        return token;
    }

    public RefreshToken verify(String token) {
        RefreshToken refreshToken = refreshTokenRepository
            .findByTokenHash(secureTokenService.hash(token))
            .orElseThrow(InvalidTokenException::new);

        if (!refreshToken.isUsable(LocalDateTime.now())) {
            throw new InvalidTokenException();
        }

        return refreshToken;
    }

    public void revoke(RefreshToken refreshToken) {
        refreshToken.setRevokedAt(LocalDateTime.now());

        refreshTokenRepository.save(refreshToken);
    }

    public void revokeQuietly(String token) {
        refreshTokenRepository
            .findByTokenHash(secureTokenService.hash(token))
            .filter(refreshToken -> refreshToken.getRevokedAt() == null)
            .ifPresent(this::revoke);
    }

    public void revokeAllOf(Long userId) {
        LocalDateTime now = LocalDateTime.now();

        List<RefreshToken> active = refreshTokenRepository
            .findByUserIdAndRevokedAtIsNull(userId);

        active.forEach(refreshToken -> refreshToken.setRevokedAt(now));

        refreshTokenRepository.saveAll(active);
    }
}
