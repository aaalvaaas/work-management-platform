package ru.itmo.wmp.platform.dto.mailbox;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConnectMailboxRequest(
    @NotBlank(message = "Host is required")
    @Size(max = 255, message = "Host must be at most 255 characters")
    String host,

    @NotNull(message = "Port is required")
    @Min(value = 1, message = "Port must be between 1 and 65535")
    @Max(value = 65535, message = "Port must be between 1 and 65535")
    Integer port,

    @NotBlank(message = "Username is required")
    @Size(max = 255, message = "Username must be at most 255 characters")
    String username,

    @Size(max = 255, message = "Password must be at most 255 characters")
    String password,

    @Size(max = 100, message = "Folder must be at most 100 characters")
    String folder,

    boolean useSsl
) {
}
