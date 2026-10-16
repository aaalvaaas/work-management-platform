package ru.itmo.wmp.platform.dto.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddBlockMemberRequest(
    @NotBlank(message = "ISU number is required")
    @Pattern(regexp = "[1-9][0-9]{5}", message = "ISU number must contain 6 digits")
    String isuNumber
) {
}
