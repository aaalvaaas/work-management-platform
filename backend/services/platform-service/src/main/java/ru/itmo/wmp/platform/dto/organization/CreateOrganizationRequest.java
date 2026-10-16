package ru.itmo.wmp.platform.dto.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
    @NotBlank(message = "Code is required")
    @Pattern(
        regexp = "[a-z0-9][a-z0-9-]{1,49}",
        message = "Code must contain lowercase letters, digits and hyphens"
    )
    String code,

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be at most 255 characters")
    String name
) {
}
