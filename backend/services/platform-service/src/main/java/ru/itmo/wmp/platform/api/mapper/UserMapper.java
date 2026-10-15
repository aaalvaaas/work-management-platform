package ru.itmo.wmp.platform.api.mapper;

import org.springframework.stereotype.Component;
import ru.itmo.wmp.platform.domain.GlobalRole;
import ru.itmo.wmp.platform.domain.User;
import ru.itmo.wmp.platform.dto.response.UserResponse;

import java.util.Set;

@Component
public class UserMapper {
    public UserResponse toResponse(User user, Set<GlobalRole> globalRoles) {
        return new UserResponse(
            user.getId(),
            user.getIsuNumber(),
            user.getLastName(),
            user.getFirstName(),
            user.getPatronymic(),
            user.getEmail(),
            user.getPosition(),
            user.getStatus(),
            globalRoles,
            user.getCreatedAt()
        );
    }
}
