package ru.itmo.wmp.platform.dto;

public record UserRequest(
    String username,
    String email
) {
}
