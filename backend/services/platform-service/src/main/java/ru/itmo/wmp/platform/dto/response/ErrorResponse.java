package ru.itmo.wmp.platform.dto.response;

import java.util.Map;

public record ErrorResponse(
    String message,
    Map<String, String> errors
) {
}
