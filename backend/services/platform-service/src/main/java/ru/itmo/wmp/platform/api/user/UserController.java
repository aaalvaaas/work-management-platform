package ru.itmo.wmp.platform.api.user;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.itmo.wmp.platform.api.user.mapper.UserMapper;
import ru.itmo.wmp.platform.application.user.UserService;
import ru.itmo.wmp.platform.config.security.JwtPrincipal;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.dto.user.UserProfileRequest;
import ru.itmo.wmp.platform.dto.user.UserResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(
        summary = "Get all users",
        description = "Returns list of registered platform accounts"
    )
    public List<UserResponse> getUsers() {
        return userService.findAll()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @GetMapping("/me")
    @Operation(
        summary = "Get own profile"
    )
    public UserResponse getOwnProfile(
        @AuthenticationPrincipal Jwt jwt
    ) {
        return toResponse(userService.getById(JwtPrincipal.userId(jwt)));
    }

    @PatchMapping("/me")
    @Operation(
        summary = "Update own profile",
        description = "Updates only the fields a user is allowed to change"
    )
    public UserResponse updateOwnProfile(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody UserProfileRequest request
    ) {
        return toResponse(
            userService.updateProfile(
                JwtPrincipal.userId(jwt),
                request.patronymic(),
                request.position()
            )
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("@accessGuard.isSelfOrPlatformAdmin(#id, authentication)")
    @Operation(
        summary = "Get user by id"
    )
    public UserResponse getById(
        @PathVariable Long id
    ) {
        return toResponse(userService.getById(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@accessGuard.isSelfOrPlatformAdmin(#id, authentication)")
    @Operation(
        summary = "Update user profile"
    )
    public UserResponse updateProfile(
        @PathVariable Long id,
        @Valid @RequestBody UserProfileRequest request
    ) {
        User updated = userService.updateProfile(
            id,
            request.patronymic(),
            request.position()
        );

        return toResponse(updated);
    }

    private UserResponse toResponse(User user) {
        return userMapper.toResponse(
            user,
            userService.rolesOf(user.getId())
        );
    }
}
