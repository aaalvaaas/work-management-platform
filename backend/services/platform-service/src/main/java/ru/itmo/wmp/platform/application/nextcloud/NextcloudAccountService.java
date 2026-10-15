package ru.itmo.wmp.platform.application.nextcloud;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.wmp.platform.application.nextcloud.repository.NextcloudAccountRepository;
import ru.itmo.wmp.platform.application.security.SecureTokenService;
import ru.itmo.wmp.platform.domain.nextcloud.NextcloudAccount;
import ru.itmo.wmp.platform.domain.nextcloud.NextcloudAccountStatus;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.exception.nextcloud.NextcloudException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NextcloudAccountService {
    private final NextcloudAccountRepository nextcloudAccountRepository;
    private final NextcloudUserGateway nextcloudUserGateway;
    private final SecureTokenService secureTokenService;

    @Transactional(readOnly = true)
    public Optional<NextcloudAccount> findByUserId(Long userId) {
        return nextcloudAccountRepository.findByUserId(userId);
    }

    public NextcloudAccount provision(User user) {
        NextcloudAccount account = accountOf(user);

        return sync(account, () -> {
            nextcloudUserGateway.create(
                nextcloudUser(user),
                secureTokenService.generate()
            );

            return NextcloudAccountStatus.ACTIVE;
        });
    }

    public NextcloudAccount syncProfile(User user) {
        Optional<NextcloudAccount> existing = nextcloudAccountRepository
            .findByUserId(user.getId());

        if (existing.isEmpty()) {
            return provision(user);
        }

        NextcloudAccount account = existing.get();

        return sync(account, () -> {
            nextcloudUserGateway.update(nextcloudUser(user));

            return account.getStatus() == NextcloudAccountStatus.DISABLED
                ? NextcloudAccountStatus.DISABLED
                : NextcloudAccountStatus.ACTIVE;
        });
    }

    public Optional<NextcloudAccount> disable(User user) {
        return nextcloudAccountRepository.findByUserId(user.getId())
            .map(account -> sync(account, () -> {
                nextcloudUserGateway.disable(account.getNextcloudUid());

                return NextcloudAccountStatus.DISABLED;
            }));
    }

    public Optional<NextcloudAccount> enable(User user) {
        return nextcloudAccountRepository.findByUserId(user.getId())
            .map(account -> sync(account, () -> {
                nextcloudUserGateway.enable(account.getNextcloudUid());

                return NextcloudAccountStatus.ACTIVE;
            }));
    }

    private NextcloudAccount sync(
        NextcloudAccount account,
        Supplier<NextcloudAccountStatus> operation
    ) {
        try {
            account.setStatus(operation.get());
            account.setLastError(null);
            account.setLastSyncedAt(LocalDateTime.now());
        } catch (NextcloudException exception) {
            account.setStatus(NextcloudAccountStatus.FAILED);
            account.setLastError(shorten(exception.getMessage()));

            log.warn(
                "Nextcloud sync failed for uid={}: {}",
                account.getNextcloudUid(),
                exception.getMessage()
            );
        }

        return nextcloudAccountRepository.save(account);
    }

    private NextcloudAccount accountOf(User user) {
        return nextcloudAccountRepository.findByUserId(user.getId())
            .orElseGet(() -> {
                NextcloudAccount account = new NextcloudAccount();

                account.setUser(user);
                account.setNextcloudUid(user.getIsuNumber());
                account.setStatus(NextcloudAccountStatus.PENDING);

                return account;
            });
    }

    private NextcloudUser nextcloudUser(User user) {
        return new NextcloudUser(
            user.getIsuNumber(),
            user.fullName(),
            user.getEmail()
        );
    }

    private String shorten(String message) {
        if (message == null) {
            return null;
        }

        return message.length() <= NextcloudAccount.LAST_ERROR_MAX_LENGTH
            ? message
            : message.substring(0, NextcloudAccount.LAST_ERROR_MAX_LENGTH);
    }
}
