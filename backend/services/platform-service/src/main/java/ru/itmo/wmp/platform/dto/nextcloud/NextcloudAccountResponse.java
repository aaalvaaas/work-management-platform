package ru.itmo.wmp.platform.dto.nextcloud;

import ru.itmo.wmp.platform.domain.nextcloud.NextcloudAccountStatus;

import java.time.LocalDateTime;

public record NextcloudAccountResponse(
    Long userId,
    String nextcloudUid,
    NextcloudAccountStatus status,
    String lastError,
    LocalDateTime lastSyncedAt
) {
}
