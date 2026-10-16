package ru.itmo.wmp.platform.api.auth;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.wmp.platform.api.auth.mapper.TokenMapper;
import ru.itmo.wmp.platform.application.auth.AuthService;
import ru.itmo.wmp.platform.application.auth.AuthTokens;
import ru.itmo.wmp.platform.dto.auth.LoginRequest;
import ru.itmo.wmp.platform.dto.auth.TokenResponse;
import ru.itmo.wmp.platform.exception.auth.InvalidTokenException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final TokenMapper tokenMapper;
    private final RefreshTokenCookie refreshTokenCookie;

    @PostMapping("/login")
    @Operation(
        summary = "Log in",
        description = "Returns an access token and sets the refresh token as an http-only cookie"
    )
    public ResponseEntity<TokenResponse> login(
        @Valid @RequestBody LoginRequest request
    ) {
        return withRefreshCookie(
            authService.login(request.isuNumber(), request.password())
        );
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh tokens",
        description = "Reads the refresh token from the cookie, rotates it and issues a new access token"
    )
    public ResponseEntity<TokenResponse> refresh(
        @CookieValue(name = "${auth.refresh-cookie.name}", required = false) String refreshToken
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidTokenException();
        }

        return withRefreshCookie(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Log out",
        description = "Revokes the refresh token from the cookie and the access token sent with the request"
    )
    public ResponseEntity<Void> logout(
        @CookieValue(name = "${auth.refresh-cookie.name}", required = false) String refreshToken,
        @AuthenticationPrincipal Jwt accessToken
    ) {
        authService.logout(refreshToken, tokenMapper.toIssuedAccessToken(accessToken));

        return ResponseEntity
            .noContent()
            .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.clear().toString())
            .build();
    }

    private ResponseEntity<TokenResponse> withRefreshCookie(AuthTokens tokens) {
        ResponseCookie cookie = refreshTokenCookie.issue(tokens.refreshToken());

        return ResponseEntity
            .ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(tokenMapper.toResponse(tokens));
    }
}
