package ru.itmo.wmp.platform.infrastructure.nextcloud;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.itmo.wmp.platform.application.nextcloud.NextcloudGroupGateway;

import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(
    prefix = "nextcloud",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true
)
public class DisabledNextcloudGroupGateway implements NextcloudGroupGateway {
    @Override
    public void ensureGroup(String groupId) {
        log.debug("Nextcloud integration is disabled, skipping group {}", groupId);
    }

    @Override
    public List<String> groupsOf(String uid) {
        return List.of();
    }

    @Override
    public void addUserToGroup(String uid, String groupId) {
        log.debug("Nextcloud integration is disabled, skipping adding {} to {}", uid, groupId);
    }

    @Override
    public void removeUserFromGroup(String uid, String groupId) {
        log.debug("Nextcloud integration is disabled, skipping removing {} from {}", uid, groupId);
    }
}
