package ru.itmo.wmp.platform.api.nextcloud.mapper;

import org.springframework.stereotype.Component;
import ru.itmo.wmp.platform.domain.nextcloud.NextcloudAccount;
import ru.itmo.wmp.platform.dto.nextcloud.NextcloudAccountResponse;

@Component
public class NextcloudAccountMapper {
    public NextcloudAccountResponse toResponse(NextcloudAccount account) {
        return new NextcloudAccountResponse(
            account.getUser().getId(),
            account.getNextcloudUid(),
            account.getStatus(),
            account.getLastError(),
            account.getLastSyncedAt()
        );
    }
}
