package ru.itmo.wmp.platform.api;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.itmo.wmp.platform.api.mapper.UserMapper;
import ru.itmo.wmp.platform.application.UserService;
import ru.itmo.wmp.platform.domain.User;
import ru.itmo.wmp.platform.dto.request.UserRequest;
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
        description = "Returns list of registered users"
    )
    public List<UserResponse> getUsers() {
        return userService.findAll()
            .stream()
            .map(userMapper::toResponse)
            .toList();
    }

    @PostMapping
    @Operation(
        summary = "Create user"
    )
    public UserResponse create(
        @Valid @RequestBody UserRequest request
        ) {
        User user = userMapper.toEntity(request);

        User saved = userService.save(user);

        return userMapper.toResponse(saved);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get user by id"
    )
    public UserResponse getById(
        @PathVariable Long id
    ) {
        User user = userService.getById(id);

        return userMapper.toResponse(user);
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update user by id"
    )
    public UserResponse update(
        @PathVariable Long id,
        @Valid @RequestBody UserRequest request
    ) {
        User user = userMapper.toEntity(request);

        User updated = userService.update(id, user);

        return userMapper.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete user by id"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
        @PathVariable Long id
    ) {
        userService.delete(id);
    }
}
