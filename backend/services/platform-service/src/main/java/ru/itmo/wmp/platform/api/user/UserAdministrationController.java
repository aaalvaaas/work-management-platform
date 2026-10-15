package ru.itmo.wmp.platform.api.user;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.itmo.wmp.platform.api.user.mapper.UserMapper;
import ru.itmo.wmp.platform.application.user.UserAdministrationService;
import ru.itmo.wmp.platform.application.user.UserService;
import ru.itmo.wmp.platform.config.security.JwtPrincipal;
import ru.itmo.wmp.platform.domain.user.GlobalRole;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.dto.user.UserResponse;

@RestController
@RequestMapping("/api/v1/users/{id}")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@RequiredArgsConstructor
public class UserAdministrationController {
    private final UserAdministrationService userAdministrationService;
    private final UserService userService;
    private final UserMapper userMapper;

    @PostMapping("/block")
    @Operation(
        summary = "Block account",
        description = "Blocks the account on the whole platform without touching organization membership"
    )
    public UserResponse block(
        @PathVariable Long id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return toResponse(userAdministrationService.block(id, JwtPrincipal.userId(jwt)));
    }

    @PostMapping("/unblock")
    @Operation(
        summary = "Unblock account"
    )
    public UserResponse unblock(
        @PathVariable Long id
    ) {
        return toResponse(userAdministrationService.unblock(id));
    }

    @PutMapping("/global-roles/{role}")
    @Operation(
        summary = "Grant global role"
    )
    public UserResponse grantRole(
        @PathVariable Long id,
        @PathVariable GlobalRole role,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return toResponse(
            userAdministrationService.grantRole(id, role, JwtPrincipal.userId(jwt))
        );
    }

    @DeleteMapping("/global-roles/{role}")
    @Operation(
        summary = "Revoke global role"
    )
    public UserResponse revokeRole(
        @PathVariable Long id,
        @PathVariable GlobalRole role,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return toResponse(
            userAdministrationService.revokeRole(id, role, JwtPrincipal.userId(jwt))
        );
    }

    private UserResponse toResponse(User user) {
        return userMapper.toResponse(
            user,
            userService.rolesOf(user.getId())
        );
    }
}
