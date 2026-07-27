package ru.itmo.wmp.platform.dto;

public record UserResponse(
    Long id,
    String username,
    String email
) {
}
