package ru.itmo.wmp.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
    @NotBlank(message = "ISU number is required")
    @Pattern(regexp = "[1-9][0-9]{5}", message = "ISU number must contain 6 digits")
    String isuNumber,

    @NotBlank(message = "Password is required")
    String password
) {
}
