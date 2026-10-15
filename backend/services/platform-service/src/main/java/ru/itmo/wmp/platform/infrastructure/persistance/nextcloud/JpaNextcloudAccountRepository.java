package ru.itmo.wmp.platform.infrastructure.persistance.nextcloud;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.wmp.platform.domain.nextcloud.NextcloudAccount;

import java.util.Optional;

public interface JpaNextcloudAccountRepository extends JpaRepository<NextcloudAccount, Long> {
    Optional<NextcloudAccount> findByUserId(Long userId);

    Optional<NextcloudAccount> findByNextcloudUid(String nextcloudUid);
}
