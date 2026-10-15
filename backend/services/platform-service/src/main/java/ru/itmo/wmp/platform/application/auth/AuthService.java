package ru.itmo.wmp.platform.application.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.wmp.platform.application.user.UserService;
import ru.itmo.wmp.platform.application.user.repository.UserRepository;
import ru.itmo.wmp.platform.domain.user.GlobalRole;
import ru.itmo.wmp.platform.domain.auth.RefreshToken;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.exception.auth.AccountBlockedException;
import ru.itmo.wmp.platform.exception.auth.InvalidCredentialsException;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenService accessTokenService;
    private final RefreshTokenService refreshTokenService;

    public AuthTokens login(String isuNumber, String password) {
        User user = userRepository.findByIsuNumber(isuNumber)
            .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        if (!user.isActive()) {
            throw new AccountBlockedException(user.getIsuNumber());
        }

        return issueTokens(user);
    }

    public AuthTokens refresh(String token) {
        RefreshToken refreshToken = refreshTokenService.verify(token);

        User user = refreshToken.getUser();

        if (!user.isActive()) {
            refreshTokenService.revokeAllOf(user.getId());

            throw new AccountBlockedException(user.getIsuNumber());
        }

        refreshTokenService.revoke(refreshToken);

        return issueTokens(user);
    }

    public void logout(String token) {
        refreshTokenService.revokeQuietly(token);
    }

    private AuthTokens issueTokens(User user) {
        Set<GlobalRole> globalRoles = userService.rolesOf(user.getId());

        return new AuthTokens(
            accessTokenService.issue(user, globalRoles),
            refreshTokenService.issue(user),
            accessTokenService.accessTokenTtl().toSeconds()
        );
    }
}
