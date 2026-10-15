package ru.itmo.wmp.platform.infrastructure.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.itmo.wmp.platform.application.notification.RegistrationNotification;
import ru.itmo.wmp.platform.application.notification.RegistrationNotifier;

@Slf4j
@Component
public class LoggingRegistrationNotifier implements RegistrationNotifier {
    @Override
    public void sendEmailConfirmation(RegistrationNotification notification) {
        log.info(
            "Email confirmation for isuNumber={} to={} token={} expiresAt={}",
            notification.isuNumber(),
            notification.email(),
            notification.token(),
            notification.expiresAt()
        );
    }

    @Override
    public void sendInvitation(RegistrationNotification notification) {
        log.info(
            "Invitation to organization={} for isuNumber={} to={} token={} expiresAt={}",
            notification.organizationName(),
            notification.isuNumber(),
            notification.email(),
            notification.token(),
            notification.expiresAt()
        );
    }
}
