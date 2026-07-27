package ru.itmo.wmp.platform.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.itmo.wmp.platform.application.UserService;
import ru.itmo.wmp.platform.domain.User;
import ru.itmo.wmp.platform.dto.UserRequest;
import ru.itmo.wmp.platform.dto.UserResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public List<UserResponse> getUsers() {
        return userService.findAll()
            .stream()
            .map(user ->
                new UserResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail()
                )
            )
            .toList();
    }

    @PostMapping
    public UserResponse create(
        @RequestBody UserRequest request
        ) {
        User user = new User();

        user.setUsername(request.username());
        user.setEmail(request.email());

        User saved = userService.save(user);

        return new UserResponse(
            saved.getId(),
            saved.getUsername(),
            saved.getEmail()
        );
    }
}
