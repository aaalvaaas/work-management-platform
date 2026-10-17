package ru.itmo.wmp.platform.dto.registration;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SubmitRegistrationRequest(
    @NotBlank(message = "Organization code is required")
    @Pattern(
        regexp = "[a-z0-9][a-z0-9-]{1,49}",
        message = "Code must contain lowercase letters, digits and hyphens"
    )
    String organizationCode,

    @NotBlank(message = "ISU number is required")
    @Pattern(regexp = "[1-9][0-9]{5}", message = "ISU number must contain 6 digits")
    String isuNumber,

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must be at most 100 characters")
    String lastName,

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must be at most 100 characters")
    String firstName,

    @Size(max = 100, message = "Patronymic must be at most 100 characters")
    String patronymic,

    @NotBlank(message = "Email is required")
    @Email(message = "Email is not valid")
    @Size(max = 255, message = "Email must be at most 255 characters")
    String email,

    @Size(max = 255, message = "Position must be at most 255 characters")
    String position,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    String password
) {
}
