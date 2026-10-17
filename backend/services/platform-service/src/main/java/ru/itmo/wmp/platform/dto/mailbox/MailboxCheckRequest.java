package ru.itmo.wmp.platform.dto.mailbox;

import jakarta.validation.constraints.Size;

public record MailboxCheckRequest(
    boolean successful,

    @Size(max = 500, message = "Error must be at most 500 characters")
    String error
) {
}
