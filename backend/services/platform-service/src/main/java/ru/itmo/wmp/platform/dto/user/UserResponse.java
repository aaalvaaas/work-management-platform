package ru.itmo.wmp.platform.dto.user;

import ru.itmo.wmp.platform.domain.user.GlobalRole;
import ru.itmo.wmp.platform.domain.user.UserStatus;

import java.time.LocalDateTime;
import java.util.Set;

public record UserResponse(
    Long id,
    String isuNumber,
    String lastName,
    String firstName,
    String patronymic,
    String email,
    String position,
    UserStatus status,
    Set<GlobalRole> globalRoles,
    LocalDateTime createdAt
) {
}
