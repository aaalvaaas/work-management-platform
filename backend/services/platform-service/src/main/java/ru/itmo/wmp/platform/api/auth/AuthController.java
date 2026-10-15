package ru.itmo.wmp.platform.api.auth;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.itmo.wmp.platform.api.auth.mapper.TokenMapper;
import ru.itmo.wmp.platform.application.auth.AuthService;
import ru.itmo.wmp.platform.dto.auth.LoginRequest;
import ru.itmo.wmp.platform.dto.auth.RefreshTokenRequest;
import ru.itmo.wmp.platform.dto.auth.TokenResponse;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final TokenMapper tokenMapper;

    @PostMapping("/login")
    @Operation(
        summary = "Log in",
        description = "Returns an access token and a refresh token for an ISU number and password"
    )
    public TokenResponse login(
        @Valid @RequestBody LoginRequest request
    ) {
        return tokenMapper.toResponse(
            authService.login(request.isuNumber(), request.password())
        );
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh tokens",
        description = "Rotates the refresh token and issues a new access token"
    )
    public TokenResponse refresh(
        @Valid @RequestBody RefreshTokenRequest request
    ) {
        return tokenMapper.toResponse(
            authService.refresh(request.refreshToken())
        );
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Log out",
        description = "Revokes the refresh token"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
        @Valid @RequestBody RefreshTokenRequest request
    ) {
        authService.logout(request.refreshToken());
    }
}
