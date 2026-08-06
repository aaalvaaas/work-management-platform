package ru.itmo.wmp.platform.dto.response;

public record UserResponse(
    Long id,
    String username,
    String email
) {
}
