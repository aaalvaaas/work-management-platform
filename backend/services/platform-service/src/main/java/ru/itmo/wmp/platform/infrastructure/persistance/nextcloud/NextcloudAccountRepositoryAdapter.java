package ru.itmo.wmp.platform.infrastructure.persistance.nextcloud;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.itmo.wmp.platform.application.nextcloud.repository.NextcloudAccountRepository;
import ru.itmo.wmp.platform.domain.nextcloud.NextcloudAccount;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NextcloudAccountRepositoryAdapter implements NextcloudAccountRepository {
    private final JpaNextcloudAccountRepository nextcloudAccountRepository;

    @Override
    public Optional<NextcloudAccount> findByUserId(Long userId) {
        return nextcloudAccountRepository.findByUserId(userId);
    }

    @Override
    public Optional<NextcloudAccount> findByNextcloudUid(String nextcloudUid) {
        return nextcloudAccountRepository.findByNextcloudUid(nextcloudUid);
    }

    @Override
    public NextcloudAccount save(NextcloudAccount account) {
        return nextcloudAccountRepository.save(account);
    }
}
