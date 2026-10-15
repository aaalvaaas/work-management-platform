package ru.itmo.wmp.platform.application.nextcloud.repository;

import ru.itmo.wmp.platform.domain.nextcloud.NextcloudAccount;

import java.util.Optional;

public interface NextcloudAccountRepository {
    Optional<NextcloudAccount> findByUserId(Long userId);

    Optional<NextcloudAccount> findByNextcloudUid(String nextcloudUid);

    NextcloudAccount save(NextcloudAccount account);
}
