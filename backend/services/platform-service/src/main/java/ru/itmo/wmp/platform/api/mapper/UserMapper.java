package ru.itmo.wmp.platform.api.mapper;

import org.springframework.stereotype.Component;
import ru.itmo.wmp.platform.domain.User;
import ru.itmo.wmp.platform.dto.request.UserRequest;
import ru.itmo.wmp.platform.dto.response.UserResponse;

@Component
public class UserMapper {
    public UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail()
        );
    }

    public User toEntity(UserRequest request) {
        User user = new User();

        user.setUsername(request.username());
        user.setEmail(request.email());

        return user;
    }
}
