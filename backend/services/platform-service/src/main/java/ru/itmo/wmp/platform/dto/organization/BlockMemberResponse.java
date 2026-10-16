package ru.itmo.wmp.platform.dto.organization;

import java.time.LocalDateTime;

public record BlockMemberResponse(
    Long participantId,
    String isuNumber,
    String lastName,
    String firstName,
    String patronymic,
    String email,
    String position,
    boolean registered,
    LocalDateTime addedAt
) {
}
