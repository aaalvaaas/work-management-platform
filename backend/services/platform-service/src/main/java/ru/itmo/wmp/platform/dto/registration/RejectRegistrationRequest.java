package ru.itmo.wmp.platform.dto.registration;

import jakarta.validation.constraints.Size;

public record RejectRegistrationRequest(
    @Size(max = 500, message = "Comment must be at most 500 characters")
    String comment
) {
}
