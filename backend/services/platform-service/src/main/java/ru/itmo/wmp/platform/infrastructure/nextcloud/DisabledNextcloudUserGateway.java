package ru.itmo.wmp.platform.infrastructure.nextcloud;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.itmo.wmp.platform.application.nextcloud.NextcloudUser;
import ru.itmo.wmp.platform.application.nextcloud.NextcloudUserGateway;

@Slf4j
@Component
@ConditionalOnProperty(
    prefix = "nextcloud",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true
)
public class DisabledNextcloudUserGateway implements NextcloudUserGateway {
    @Override
    public boolean exists(String uid) {
        return false;
    }

    @Override
    public void create(NextcloudUser user, String password) {
        log.debug("Nextcloud integration is disabled, skipped create for uid={}", user.uid());
    }

    @Override
    public void update(NextcloudUser user) {
        log.debug("Nextcloud integration is disabled, skipped update for uid={}", user.uid());
    }

    @Override
    public void enable(String uid) {
        log.debug("Nextcloud integration is disabled, skipped enable for uid={}", uid);
    }

    @Override
    public void disable(String uid) {
        log.debug("Nextcloud integration is disabled, skipped disable for uid={}", uid);
    }
}
