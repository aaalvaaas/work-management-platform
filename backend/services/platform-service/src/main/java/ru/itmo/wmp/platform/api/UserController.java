package ru.itmo.wmp.platform.api;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.itmo.wmp.platform.api.mapper.UserMapper;
import ru.itmo.wmp.platform.application.UserService;
import ru.itmo.wmp.platform.domain.User;
import ru.itmo.wmp.platform.dto.request.UserProfileRequest;
import ru.itmo.wmp.platform.dto.response.UserResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping
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

    @GetMapping("/{id}")
    @Operation(
        summary = "Get user by id"
    )
    public UserResponse getById(
        @PathVariable Long id
    ) {
        return toResponse(userService.getById(id));
    }

    @PatchMapping("/{id}")
    @Operation(
        summary = "Update user profile",
        description = "Updates only the fields a user is allowed to change"
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
