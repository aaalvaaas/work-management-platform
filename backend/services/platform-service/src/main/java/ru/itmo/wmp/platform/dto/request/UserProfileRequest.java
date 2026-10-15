package ru.itmo.wmp.platform.dto.request;

import jakarta.validation.constraints.Size;

public record UserProfileRequest(
    @Size(max = 100, message = "Patronymic must be at most 100 characters")
    String patronymic,

    @Size(max = 255, message = "Position must be at most 255 characters")
    String position
) {
}
