package ru.itmo.wmp.platform.application.notification;

public interface RegistrationNotifier {
    void sendEmailConfirmation(RegistrationNotification notification);

    void sendInvitation(RegistrationNotification notification);
}
