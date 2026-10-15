package ru.itmo.wmp.platform.application.nextcloud;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.wmp.platform.application.nextcloud.repository.NextcloudAccountRepository;
import ru.itmo.wmp.platform.application.security.SecureTokenService;
import ru.itmo.wmp.platform.domain.nextcloud.NextcloudAccount;
import ru.itmo.wmp.platform.domain.nextcloud.NextcloudAccountStatus;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.exception.nextcloud.NextcloudException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NextcloudAccountServiceTest {
    @Mock
    private NextcloudAccountRepository nextcloudAccountRepository;

    @Mock
    private NextcloudUserGateway nextcloudUserGateway;

    @Mock
    private SecureTokenService secureTokenService;

    @InjectMocks
    private NextcloudAccountService nextcloudAccountService;

    @BeforeEach
    void setUp() {
        when(nextcloudAccountRepository.save(any(NextcloudAccount.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldProvisionAccountWithIsuNumberAsUid() {
        when(nextcloudAccountRepository.findByUserId(1L))
            .thenReturn(Optional.empty());

        when(secureTokenService.generate())
            .thenReturn("generated-password");

        NextcloudAccount account = nextcloudAccountService.provision(user());

        assertEquals("465824", account.getNextcloudUid());
        assertEquals(NextcloudAccountStatus.ACTIVE, account.getStatus());
        assertNotNull(account.getLastSyncedAt());
        assertNull(account.getLastError());

        verify(nextcloudUserGateway).create(
            new NextcloudUser("465824", "Иванов Иван", "465824@niuitmo.ru"),
            "generated-password"
        );
    }

    @Test
    void shouldKeepRegistrationAliveWhenNextcloudIsUnavailable() {
        when(nextcloudAccountRepository.findByUserId(1L))
            .thenReturn(Optional.empty());

        when(secureTokenService.generate())
            .thenReturn("generated-password");

        doThrow(new NextcloudException("Nextcloud is not reachable"))
            .when(nextcloudUserGateway)
            .create(any(NextcloudUser.class), any());

        NextcloudAccount account = nextcloudAccountService.provision(user());

        assertEquals(NextcloudAccountStatus.FAILED, account.getStatus());
        assertEquals("Nextcloud is not reachable", account.getLastError());
    }

    @Test
    void shouldDisableAccount() {
        when(nextcloudAccountRepository.findByUserId(1L))
            .thenReturn(Optional.of(account(NextcloudAccountStatus.ACTIVE)));

        NextcloudAccount disabled = nextcloudAccountService.disable(user()).orElseThrow();

        assertEquals(NextcloudAccountStatus.DISABLED, disabled.getStatus());

        verify(nextcloudUserGateway).disable("465824");
    }

    @Test
    void shouldEnableAccount() {
        when(nextcloudAccountRepository.findByUserId(1L))
            .thenReturn(Optional.of(account(NextcloudAccountStatus.DISABLED)));

        NextcloudAccount enabled = nextcloudAccountService.enable(user()).orElseThrow();

        assertEquals(NextcloudAccountStatus.ACTIVE, enabled.getStatus());

        verify(nextcloudUserGateway).enable("465824");
    }

    @Test
    void shouldKeepDisabledStateOnProfileSync() {
        when(nextcloudAccountRepository.findByUserId(1L))
            .thenReturn(Optional.of(account(NextcloudAccountStatus.DISABLED)));

        NextcloudAccount synced = nextcloudAccountService.syncProfile(user());

        assertEquals(NextcloudAccountStatus.DISABLED, synced.getStatus());

        verify(nextcloudUserGateway).update(any(NextcloudUser.class));
    }

    private NextcloudAccount account(NextcloudAccountStatus status) {
        NextcloudAccount account = new NextcloudAccount();

        account.setUser(user());
        account.setNextcloudUid("465824");
        account.setStatus(status);

        return account;
    }

    private User user() {
        User user = new User();

        user.setId(1L);
        user.setIsuNumber("465824");
        user.setEmail("465824@niuitmo.ru");
        user.setLastName("Иванов");
        user.setFirstName("Иван");
        user.setPasswordHash("hash");

        return user;
    }
}
